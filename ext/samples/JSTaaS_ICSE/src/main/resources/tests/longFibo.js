function add(a, b) 
{
    while (a.length < b.length) a.unshift(0);
    while (a.length > b.length) b.unshift(0);
    var carry = 0, sum = []
    for (var i = a.length - 1; i >= 0; i--) {
        var s = a[i] + b[i] + carry;
        if (s >= 10) {
            s = s - 10;
            carry = 1;
        } else {
            carry = 0;
        }
        sum.unshift(s);
    }
    if (carry)
        sum.unshift(carry);
    return sum;
}

function fibRec(n)
{
	if(n <= 2)
		return [1];
	
	return add(fibRec(n-1), fibRec(n-2));
}

function fibRecStart(n)
{
	return fibRec(n).join("");
}

fibRecStart(NUMBER);