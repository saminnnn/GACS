package codes;


/**
 * Created by samin on 12/26/16.
 */
public class SearchString {
    public char[] passage;
    int passageLength;
    final int characterCount=256;

    public SearchString(String s){
        passage=s.toUpperCase().toCharArray();
        passageLength=passage.length;

        int index=0;
        boolean space=false;
        for(int i=0;i<passageLength;i++){

            if(passage[i]==' '){
                if(!space){
                    space=true;
                    passage[index]=passage[i];
                    index++;
                }
            }
            else{
                space=false;
                passage[index]=passage[i];
                index++;
            }
            //System.out.println(index);
            //passageLength=index;
        }
    }

    public int search(String s){
    	return search(s,0);
    }
    
    public int search(String s, int start) {
    	char[] haystack=passage;
    	char[] needle=s.toUpperCase().toCharArray();
    	
        if (needle.length == 0) {
            return 0;
        }
        int charTable[] = makeCharTable(needle);
        int offsetTable[] = makeOffsetTable(needle);
        for (int i = needle.length - 1+start, j; i < haystack.length;) {
            for (j = needle.length - 1; needle[j] == haystack[i]; --i, --j) {
                if (j == 0) {
                    return i;
                }
            }
            // i += needle.length - j; // For naive method
            if(haystack[i]>255) break;
            i += Math.max(offsetTable[needle.length - 1 - j], charTable[haystack[i]]);
        }
        return -1;
    }
    
    /**
     * Makes the jump table based on the mismatched character information.
     */
    private static int[] makeCharTable(char[] needle) {
        final int ALPHABET_SIZE = 256;
        int[] table = new int[ALPHABET_SIZE];
        for (int i = 0; i < table.length; ++i) {
            table[i] = needle.length;
        }
        for (int i = 0; i < needle.length - 1; ++i) {
            table[needle[i]] = needle.length - 1 - i;
        }
        return table;
    }
    
    /**
     * Makes the jump table based on the scan offset which mismatch occurs.
     */
    private static int[] makeOffsetTable(char[] needle) {
        int[] table = new int[needle.length];
        int lastPrefixPosition = needle.length;
        for (int i = needle.length - 1; i >= 0; --i) {
            if (isPrefix(needle, i + 1)) {
                lastPrefixPosition = i + 1;
            }
            table[needle.length - 1 - i] = lastPrefixPosition - i + needle.length - 1;
        }
        for (int i = 0; i < needle.length - 1; ++i) {
            int slen = suffixLength(needle, i);
            table[slen] = needle.length - 1 - i + slen;
        }
        return table;
    }
    
    /**
     * Is needle[p:end] a prefix of needle?
     */
    private static boolean isPrefix(char[] needle, int p) {
        for (int i = p, j = 0; i < needle.length; ++i, ++j) {
            if (needle[i] != needle[j]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns the maximum length of the substring ends at p and is a suffix.
     */
    private static int suffixLength(char[] needle, int p) {
        int len = 0;
        for (int i = p, j = needle.length - 1;
                 i >= 0 && needle[i] == needle[j]; --i, --j) {
            len += 1;
        }
        return len;
    }

    /*public static void main(String[] args){
        SearchString s=new SearchString("> revenue 446,461         </td>         <td class=\"bwsinglebottom\">           ?         </td>       </tr>       <tr>         <td class=\"bwpadl6 bwpadb1 bwvertalignb bwalignl\">           Total revenue         </td>         <td>         </td>         <td>         </td>         <td class=\"bwpadl0 bwnowrap bwpadr0 bwvertalignb bwalignr bwsinglebottom\" colspan=\"2\">           1,608,419         </td>");
        System.out.print(s.search("Total revenue"));
    }*/
}
