package com.kazumaproject7.qrcodescanner.other

import timber.log.Timber

fun String.getEmailEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[2].replace(";SUB","")
    } else{
        ""
    }
}

fun String.getSubjectEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[3].replace(";BODY","")
    } else{
        ""
    }
}

fun String.getMessageEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[4].replace(";;","")
    } else{
        ""
    }
}

fun String.getEmailEmailTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2) {
                    str[0].replace("mailto:", "")
                } else{
                    ""
                }
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str1 = str[0].replace("mailto:","")
                    str1
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        if (this.contains("mailto:")){
            return when{
                this.contains("?subject=") ->{
                    val emailStr = this.replace("mailto:","")
                    val str = emailStr.split("?" ).toTypedArray()
                    if (str.size >= 2) {
                        str[0]
                    } else {
                        ""
                    }
                }
                else -> {
                    replace("mailto:", "")
                }
            }
        } else if (this.contains("MAILTO")) {
            val emailStr = this.replace("MAILTO","")
            return emailStr.replace(":","").replace(" ","")
        } else {
            return ""
        }
    }
}

fun String.getEmailSubjectTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                return ""
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str2 = str[1].split("&").toTypedArray()
                    str2[1].replace("subject=","")
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        if (this.contains("mailto:")){
            return when{
                this.contains("?subject=") ->{
                    val emailStr = this.replace("mailto:","")
                    val str = emailStr.split("?" ).toTypedArray()
                    if (str.size >= 2) {
                        str[1].replace("subject=","")
                    } else {
                        ""
                    }
                }
                else -> {
                    ""
                }
            }
        } else if (this.contains("MAILTO")) {
            return ""
        } else {
            return ""
        }
    }
}

fun String.getEmailMessageTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size == 2){
                    str[1].replace("body=","")
                }else {
                    ""
                }
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str2 = str[1].split("&").toTypedArray()
                    str2[0].replace("body=","")
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        return if (this.contains("mailto:")){
            when{
                this.contains("?subject=") ->{
                    ""
                }
                else -> {
                    ""
                }
            }
        } else if (this.contains("MAILTO")) {
            ""
        } else {
            ""
        }
    }
}

fun String.getSMSNumber():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            ""
        }
        3 ->{
            str[1]
        }
        else ->{
            ""
        }
    }
}

fun String.getSMSMessage():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            ""
        }
        3 ->{
            str[2]
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiSSID():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[3].replace(";P","")
        }
        5 ->{
            str[2]
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiPassword():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[4].replace(";H","")
        }
        5 ->{
            if (str[4].contains(";")){
                str[4].replace(";","")
            }else{
                str[4]
            }
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiStringInHistory(): String{
    val str = this.split(":")
    return when(str.size){
        3 ->{
            str[2]
        }
        else ->{
            this
        }
    }
}

fun String.getWifiEncryptionType():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[2].replace(";S","")
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiIsHidden():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            if (str[5].replace(";","") == ""){
                "false"
            } else {
                str[5].replace(";","")
            }
        }
        else ->{
            ""
        }
    }
}

fun String.getCryptocurrencyType():String {
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            str[0]
        }
        else ->{
            ""
        }
    }
}

fun String.getCryptocurrencyAddress():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            return when(str2.size){
                2 ->{
                    str2[0]
                }
                else ->{
                    ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}

fun String.getCryptocurrencyAmount():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            when(str2.size){
                2 ->{
                    val str3 = str2[1].split("&" ).toTypedArray()
                    return when(str3.size){
                        2 ->{
                            str3[0].replace("amount=","")
                        }
                        else ->{
                            ""
                        }
                    }
                }
                else ->{
                    return ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}

fun String.getCryptocurrencyMessage():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            when(str2.size){
                2 ->{
                    val str3 = str2[1].split("&" ).toTypedArray()
                    return when(str3.size){
                        2 ->{
                            str3[1].replace("message=","")
                        }
                        else ->{
                            ""
                        }
                    }
                }
                else ->{
                    return ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}

fun String.getVcardName():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("N:")->{
                return it.replace("N:","").replace(";"," ")
            }
        }
    }
    return ""
}

fun String.getVcardMobileNumber():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("TEL;CELL:")->{
                return it.replace("TEL;CELL:","")
            }
            it.contains("TEL;TYPE=CELL:")->{
                return it.replace("TEL;TYPE=CELL:","")
            }
        }
    }
    return ""
}

fun String.getVcardWorkPhoneNumber():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("TEL;WORK;VOICE:")->{
                return it.replace("TEL;WORK;VOICE:","")
            }
            it.contains("TEL:")->{
                return it.replace("TEL:","")
            }
        }
    }
    return ""
}

fun String.getVcardFaxNumber():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("TEL;FAX:")->{
                return it.replace("TEL;FAX:","")
            }
            it.contains("TEL;TYPE=FAX:")->{
                return it.replace("TEL;TYPE=FAX:","")
            }
        }
    }
    return ""
}

fun String.getVcardEmail():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        if (it.contains("EMAIL;WORK;INTERNET:")){
            return it.replace("EMAIL;WORK;INTERNET:","")
        }
        if (it.contains("EMAIL;TYPE=INTERNET:")){
            return it.replace("EMAIL;TYPE=INTERNET:","")
        }
    }
    return ""
}

fun String.getVcardAddress():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[0] + "\n" + str4[1] + "\n" + str4[2] + "\n" + str4[4]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardStreet():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[0]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardCity():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[1]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardState():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[2]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardCountry():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[4]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardZip():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ADR:;;")->{
                val str3 = it.replace("ADR:;;","")
                val str4 = str3.split(";").toTypedArray()
                return if (str4.size == 5) {
                    str4[3]
                }else{
                    ""
                }
            }
        }
    }
    return ""
}

fun String.getVcardCompanyName():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("ORG:")->{
                return it.replace("ORG:","")
            }
        }
    }
    return ""
}

fun String.getVcardCompanyTitle():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("TITLE:")->{
                return it.replace("TITLE:","")
            }
        }
    }
    return ""
}

fun String.getVcardWebsite():String {
    val str = this.replace("BEGIN:VCARD","")
        .replace("VERSION:3.0","")
        .replace("END:VCARD","")
        .replace(" ","")
    Timber.d("Vcard Text1 $str")
    val str2 = str.split("\n").toTypedArray()
    str2.forEach {
        Timber.d("Vcard text1: $it")
        when{
            it.contains("URL:")->{
                return it.replace("URL:","")
            }
        }
    }
    return ""
}