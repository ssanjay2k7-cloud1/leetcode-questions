class Solution {
    public int divide(int dividend, int divisor) {
        boolean neg= true;
        if(dividend<0 && divisor<0 || dividend>0 && divisor>0)    neg= false;

        long a= Math.abs((long)dividend), b= Math.abs((long)divisor);
        long factor=0;
        
        while(a>=b)
        {
            long temp=b, tempFactor=0;
            while((temp<<1)<=a)
            {
                temp<<=1;
                tempFactor++;
            }
            factor+= 1L<<tempFactor;
            a-= (b<<tempFactor);
        }

        if(factor>Integer.MAX_VALUE && neg)
            return Integer.MIN_VALUE;
        else if(factor>=Integer.MAX_VALUE && !neg)
            return Integer.MAX_VALUE;

        if(neg)
            return (int)-factor;
        else
            return (int)factor;
    }
}
