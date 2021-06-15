string output = "";

function concatIntString(int i, string s) {
    output = output + i.toString() + ":" + s + " ";
}

function concatIntAny(int i, any a) {
    output = output + i.toString() + ":" + a.toString() + " ";
}

// ---------------------------------------------------------------------------------------------------------------------

function testStringWithSimpleVariableWithCharType() returns string {
    output = "";

    string sdata = "Ballerina";

    int i = 0;
    foreach string:Char s in sdata {
        concatIntString(i, s);
        i += 1;
    }
    return output;
}

function testStringWithSimpleVariableWithoutType() returns string {
    output = "";

    string sdata = "Ballerina";

    int i = 0;
    foreach var s in sdata {
        concatIntString(i, s);
        i += 1;
    }
    return output;
}

function testStringWithSimpleVariableWithType() returns string {
    output = "";

    string sdata = "Ballerina";

    int i = 0;
    foreach string s in sdata {
        concatIntString(i, s);
        i += 1;
    }
    return output;
}

// ---------------------------------------------------------------------------------------------------------------------

function testStringWithSimpleVariableWithAnydataType() returns string {
    output = "";

    string sdata = "Ballerina";

    int i = 0;
    foreach anydata s in sdata {
        concatIntAny(i, s);
        i += 1;
    }
    return output;
}

// ---------------------------------------------------------------------------------------------------------------------

function testStringWithSimpleVariableWithAnyType() returns string {
    output = "";

    string sdata = "Ballerina";

    int i = 0;
    foreach any s in sdata {
        concatIntAny(i, s);
        i += 1;
    }
    return output;
}


// ---------------------------------------------------------------------------------------------------------------------

function testIterationOnEmptyString() returns string {
    output = "";

    string sdata = "";

    int i = 0;
    foreach any s in sdata {
        concatIntAny(i, s);
        i += 1;
    }
    return output;
}

// ---------------------------------------------------------------------------------------------------------------------

function testIterationTypeCheck() returns int{
    string foo = "foo";
    int count = 0;
    foreach var item in foo {
        string str = item;
        if(str is string:Char){
            count += 1;
        }
    }
    return count;
}
