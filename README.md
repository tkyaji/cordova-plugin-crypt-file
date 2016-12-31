# Cordova crypt file plugin
HTML source file is encrypted at build, and decrypted at run.  
https://www.npmjs.com/package/cordova-plugin-crypt-file

## Add Plugin
`cordova plugin add cordova-plugin-crypt-file`

## Encrypt
`cordova build [ios / android]`

## Decrypt
`cordova emulate [ios / android]`  
or  
`cordova run [ios / android]`  

## Encryption subjects.

### Default

* .html
* .htm
* .js
* .css

### Edit subjects

You can specify the encryption subjects by editing `plugin.xml`.

**plugins/cordova-plugin-crypt-file/plugin.xml**

```
<cryptfiles>
    <include>
        <file>\.(htm|html|js|css)$</file>
    </include>
    <exclude>
        <file>exclude_file\.js$</file>
    </exclude>
</cryptfiles>
```

Specify the target file as a regular expression.


## Supported platforms
* iOS
* Android
* CrossWalk

## Before reporting your issue
It would be very helpful if you show me your project (If you have GitHub repository, that URL would be nice).
It is very hard for me to reporduce your enviroment.

## License
Apache version 2.0
