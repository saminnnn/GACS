var express = require('express')
var net = require('net');
var app=express();
var http = require('http').Server(app);
var io=require('socket.io')(http);
var fs=require('fs');
var bodyParser = require('body-parser');
session = require('express-session');
var exec=require('child_process').exec;
var mosquitto=require('mqtt');

var mqtt=mosquitto.connect('tcp://127.0.0.1');

mqtt.subscribe('/gacs/headline/eps/');
mqtt.subscribe('/gacs/headline/revenue/');
mqtt.subscribe('/gacs/headline/compSales/');
mqtt.subscribe('/gacs/body/eps/');
mqtt.subscribe('/gacs/body/revenue/');

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false})); 
app.use(session({
    secret: '2C44-4D45-WppQ38S',
    resave: true,
    saveUninitialized: true
}));

var estimates=new Array(31);
for(var i=0;i<31;i++){
  estimates[i]=null;
}
var estimateIndex=0;

var auth = function(req, res, next) {
  console.log(req.session);
  var match=false;
  if(req.session){
    for(var i=0;i<usernames.length;i++){
      if(usernames[i]==req.session.user) match=true;
    }
  }

  if(match) return next();
  else res.sendFile(__dirname+'/index.html');
}

var usernames=['roberto', 'samin'];
var passwords=['rsgacs', 'samin'];


app.get('/', function(req, res){
  res.sendFile(__dirname+'/index.html');
});


app.get('/estimates', auth, function(req, res){
  res.sendFile(__dirname+'/form.html');
  setTimeout(sendAllTickers, 2000);
});

app.get('/log', auth, function(req, res){
  res.sendFile(__dirname+'/viewlog.html');
  setTimeout(firstLoad, 2000);
});

app.get('/datafeed', auth, function (req, res) {
    res.sendFile(__dirname+'/client8.html');
    setTimeout(firstLoad, 2000);
});

app.post('/dump', function(req,res){
  var ticker=req.body.tickerValue;
  var ngeps=req.body.ngepsValue;
  var geps=req.body.gepsValue;
  var revenue=req.body.revenueValue;
  var comsales=req.body.comsalesValue;
  var guepsqtr=req.body.guepsqtr1Value;
  var gurevqtr=req.body.gurevqtr1Value;
  var quarter=req.body.quarterValue;
  var year=req.body.yearValue;
  var user=req.body.user;

  var currentEstimate=[ticker,geps,ngeps,revenue,guepsqtr,gurevqtr,quarter,year,comsales,null];
  mqtt.publish('/gacs/estimateQuarter/',ticker+','+quarter+','+ngeps+','+geps+','+revenue+','+comsales+','+guepsqtr+','+gurevqtr);

  var i;
  for(var i=0;i<31;i++){
    if(estimates[i]){
      if(estimates[i][0]==ticker){
        estimates[i]=currentEstimate;
        break;
      }
    } 
  }
  if(i==31){
    estimates[estimateIndex]=currentEstimate;
    estimateIndex++;
    estimateIndex=estimateIndex%30;
  }
  //console.log(estimates);
});

app.post('/getlog', function(req,res){
  var company=req.body.ticker;
  var command='grep -w -h -r '+company+' '+path;

  exec(command, function(err,out,outerr) {
    io.emit('log',out);
  });
});

app.post('/login', function(req,res){
  var username=req.body.user;
  var password=req.body.pass;
  var loggedIn=false;

  for(var i=0;i<usernames.length;i++){
    if(usernames[i]==username){
      if(passwords[i]==password){
        loggedIn=true;
        req.session.user=username;
      }
    }
  }
  if(!loggedIn) io.emit('formAuth','0');
  else io.emit('formAuth','1');
  res.end();
});

app.get('/logout', function(req,res){
  req.session.destroy();
  res.sendFile(__dirname+'/index.html');
});

http.listen(3000, function(){
  console.log('listening on *:3000');
});

net.createServer(function(sock) {
    console.log('CONNECTED: ' + sock.remoteAddress +':'+ sock.remotePort);
    sock.on('data', function(data) {
          load(data);
        fs.writeFile('data.json',data);
    });    
}).listen(4100, '127.0.0.1');

mqtt.on('message', function(topic,message){
  var topicText=''+topic;
  var messageText=''+message;
  var messageLength=messageText.length;
  var ticker='';
  var value='';
  var quarter;
  var dataType;
  var i;

  for(i=0;i<messageLength;i++){
    var c=messageText.charAt(i);
    if(c==' ') break;
    else ticker+=c;
  }

  i+=2
  quarter=messageText.charAt(i);
  i++;

  for(;i<messageLength;i++){
    value+=messageText.charAt(i);
  }


  if(topicText.includes('eps')) dataType=0;
  else if(topicText.includes('rev')) dataType=1;
  else if(topicText.includes('comp')) dataType=2;

  for(var i=0;i<31;i++){
    if(estimates[i]){
      if(estimates[i][0]==ticker && estimates[i][6]==quarter){
        if(estimates[i][9]==null){
          var extraData=[null,null,null];
          extraData[dataType]=value;
          estimates[i][9]=extraData;
        }
        else estimates[i][9][dataType]=value;

        break;
      }
    } 
  }
  if(i==31){
    var currentEstimate=new Array(12);
    currentEstimate[0]=ticker;
    currentEstimate[6]=quarter;
    var extraData=[null,null,null];
    extraData[dataType]=value;
    currentEstimate[9]=extraData;
    estimates[estimateIndex]=currentEstimate;
    estimateIndex++;
    estimateIndex=estimateIndex%30;
  }
  firstLoad();
  //console.log(estimates);
});

firstLoad();

//~~~~~~~~~~~~~~~~~~~~~
//Functions Start Here
//~~~~~~~~~~~~~~~~~~~~~


function firstLoad(){
  fs.readFile('data.json',function(err,data){
    if(err) console.log(err);
    load(data);
  });
}

function load(data){
  var date=new Date();
  var time=''+(date.getMonth()+1)+'/'+date.getDate()+','+date.getHours()+':'+date.getMinutes()+':'+(date.getSeconds()+(date.getMilliseconds()/1000));
  fs.appendFile("serverlog.txt",time+' '+data+'\n', function(err){
    if(err) console.log(err);
  });

  var estimatesCopy=new Array(31);

  for(var i=0;i<31;i++){
    estimatesCopy[i]=estimates[i];  
  }

  var a=[];
  info=null; 
  if(data!='') info=JSON.parse(data);

  if(info!=null){
    var infoLength=info.length;
    for(var i=0;i<infoLength;i++){
      var proceed=true;

      for(var j=0;j<30;j++){
        if(estimatesCopy[j]){
          if(info[i]['ticker']==estimatesCopy[j][0]){
            proceed=false;
            if(info[i]['period']==estimatesCopy[j][6]){
              var current=new Array(7);
              current[0]=info[i]['ticker'];
              current[2]=info[i]['period'];

              var inputData=[];
              var inputDataLength=info[i]['data'].length;
              for(var k=0;k<inputDataLength;k++){
                var currentInput=new Array(2);

                currentInput[0]=info[i]['data'][k]['fieldtype'];
                currentInput[1]=info[i]['data'][k]['fieldvalue'];

                inputData.push(currentInput);
              }

              if(estimatesCopy[j][9]!=null){
                if(estimatesCopy[j][9][0]!=null) inputData.push([3,estimates[j][9][0]]);
                if(estimatesCopy[j][9][1]!=null) inputData.push([4,estimates[j][9][1]]);
                if(estimatesCopy[j][9][2]!=null) inputData.push([5,estimates[j][9][2]]);
              }

              var estimateData=[];
              if(estimatesCopy[j][1]) estimateData.push([0,estimates[j][1]]);
              if(estimatesCopy[j][2]) estimateData.push([1,estimates[j][2]]);
              if(estimatesCopy[j][3]) estimateData.push([2,estimates[j][3]]);
              if(estimatesCopy[j][4]) estimateData.push([3,estimates[j][4]]);
              if(estimatesCopy[j][5]) estimateData.push([4,estimates[j][5]]);
              if(estimatesCopy[j][8]) estimateData.push([5,estimates[j][8]]);


              current[1]=estimatesCopy[j][7];
              current[2]=estimatesCopy[j][6];

              estimatesCopy[j]=null; 
              
              
              current[3]=inputData;
              current[4]=estimateData;
              a.push(current);
              //console.log(current);

              for(var k=0;k<infoLength;k++){
                if(info[i]['ticker']==info[k]['ticker']) info[k]['period']=null;
              }
              break;  
            }
            
          }
        }
      }

      if(proceed){
        if(info[i]['period']=='1' || info[i]['period']=='4' || info[i]['period']=='3' || info[i]['period']=='2'){
          for(var j=i+1;j<infoLength;j++){
            if(info[j]['ticker']==info[i]['ticker']) info[j]['period']='0';
          }
          
          var current=new Array(6);
          current[0]=info[i]['ticker'];
          current[2]=info[i]['period'];
          var inputData=[];
          var inputDataLength=info[i]['data'].length;

          for(var j=0;j<inputDataLength;j++){
            var currentInput=new Array(2);

            currentInput[0]=info[i]['data'][j]['fieldtype'];
            currentInput[1]=info[i]['data'][j]['fieldvalue'];

            inputData.push(currentInput);
          }

          inputData.push(currentInput);
          current[3]=inputData;
          current[4]=[];
          a.push(current);
        }
      }
    }

    for(var i=0;i<30;i++){
      if(estimatesCopy[i]){
        current=new Array(7);
        current[0]=estimatesCopy[i][0]
        current[1]=estimatesCopy[i][7];
        current[2]=estimatesCopy[i][6];

        var estimateData=[];
        if(estimatesCopy[i][1]) estimateData.push([0,estimatesCopy[i][1]]);
        if(estimatesCopy[i][2]) estimateData.push([1,estimatesCopy[i][2]]);
        if(estimatesCopy[i][3]) estimateData.push([2,estimatesCopy[i][3]]);
        if(estimatesCopy[i][4]) estimateData.push([3,estimatesCopy[i][4]]);
        if(estimatesCopy[i][5]) estimateData.push([4,estimatesCopy[i][5]]);
        if(estimatesCopy[i][8]) estimateData.push([5,estimatesCopy[i][8]]);

        var inputData=[];
        if(estimatesCopy[i][9]!=null){
          if(estimatesCopy[i][9][0]!=null) inputData.push([3,estimates[i][9][0]]);
          if(estimatesCopy[i][9][1]!=null) inputData.push([4,estimates[i][9][1]]);
          if(estimatesCopy[i][9][2]!=null) inputData.push([5,estimates[i][9][2]]);
        }

        current[3]=inputData;
        current[4]=estimateData

        a.push(current);
      }
    }
  }
  emmiter('name',a);
}

function sendAllTickers(){
  var i=0;
  var names=[];
  var name='';
  
  fs.readFile('tickerList.txt', function(err, data){
    data=data+'\n';
    var dataLength=data.length;

    for(var i=0;i<dataLength;i++){
      var char=data.charAt(i);

      if(char=='\n'){
        names.push(name);
        name='';
      }
      else name+=char;
    }
    emmiter('allNames',names);
  });

  
}

function emmiter(name, element){
  io.emit(name, element);
}
  