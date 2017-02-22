一个分区器应该有的


    public Token midpoint(Token lToken, Token rToken)
    {
		// 1.murmur3partioner 范围是-2^63 到(2^63)-1.64bit，为什么用long型会溢出呢
	    // using BigInteger to avoid long overflow in intermediate operations
	    BigInteger l = BigInteger.valueOf(((LongToken) lToken).token),
	       r = BigInteger.valueOf(((LongToken) rToken).token),
	       midpoint;
    
	    if (l.compareTo(r) < 0)
	    {
	    BigInteger sum = l.add(r);
	    midpoint = sum.shiftRight(1);
	    }
	    else // wrapping case
	    {
	    BigInteger max = BigInteger.valueOf(MAXIMUM);
	    BigInteger min = BigInteger.valueOf(MINIMUM.token);
	    // length of range we're bisecting is (R - min) + (max - L)
	    // so we add that to L giving
	    // L + ((R - min) + (max - L) / 2) = (L + R + max - min) / 2
	    midpoint = (max.subtract(min).add(l).add(r)).shiftRight(1);
	    if (midpoint.compareTo(max) > 0)
	    midpoint = min.add(midpoint.subtract(max));
	    }
	    
	    return new LongToken(midpoint.longValue());
    }


二.

   	public void assertMidpoint(Token left, Token right, int depth)
    {
	    Random rand = new Random();
	    for (int i = 0; i < 1000; i++)
	    {
	    assertMidpoint(left, right, rand, depth);
	    }
    }
    
	//为什么要迭代随机数呢
    private void assertMidpoint(Token left, Token right, Random rand, int depth)
    {
	    Token mid = partitioner.midpoint(left, right);
	    assert new Range<Token>(left, right).contains(mid)
	    : "For " + left + "," + right + ": range did not contain mid:" + mid;
	    if (depth < 1)
	    return;
	    
	    if (rand.nextBoolean())
	    assertMidpoint(left, mid, rand, depth-1);
	    else
	    assertMidpoint(mid, right, rand, depth-1);
    }