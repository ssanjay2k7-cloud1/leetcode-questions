class Solution { 
    public boolean isPalindrome(int x) { 
        if (x < 0) { 
            return false; 
        }
        
        int reverse = 0; 
        int real = x; 
        
        while (x != 0) { 
            int last = x % 10; 
            reverse = reverse * 10 + last; 
            x = x / 10; 
        } 
        
        return (real == reverse); 
    }
}

