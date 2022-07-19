package com.kazumaproject7.qrcodescanner.other

fun getEmailEmailType1(scannedString: String): String{
    val str = scannedString.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[2].replace(";SUB","")
    } else{
        ""
    }
}

fun getSubjectEmailType1(scannedString: String): String{
    val str = scannedString.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[3].replace(";BODY","")
    } else{
        ""
    }
}

fun getBodyEmailType1(scannedString: String): String{
    val str = scannedString.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[4].replace(";;","")
    } else{
        ""
    }
}