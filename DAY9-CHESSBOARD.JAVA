class Solution {
    public boolean squareIsWhite(String coordinates) {
        
        char ch1=coordinates.charAt(0);
        int num=coordinates.charAt(1)-'0';

        if(ch1=='a'||ch1=='c'||ch1=='e'||ch1=='g'){
            if(num%2!=0){
                return false;
            }else{
                return true;
            }
        }else{
            if(num%2!=0){
                return true;
            }else{
                return false;
            }
        }
        
    }
}
