# Cordova crypt file plugin NextGen
This is an extension to [tkyaji's cordova-plugin-crypt-file](https://github.com/tkyaji/cordova-plugin-crypt-file) implementation to encrypt HTML assets during build and to decrypt the required assets during runtime.

The original implementation can also be found on https://www.npmjs.com/package/cordova-plugin-crypt-file.

## Requires node-rsa
`npm install -g node-rsa`

## Add Plugin
`cordova plugin add https://github.com/qhng/cordova-plugin-crypt-file`

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
        <file regex="\.(htm|html|js|css)$" />
    </include>
    <exclude>
        <file regex="exclude_file\.js$" />
    </exclude>
</cryptfiles>
```

Specify the target file as a regular expression.


## Supported platforms
* Android

## Before reporting your issue
It would be very helpful if you show me your project (If you have GitHub repository, that URL would be nice).
It is very hard for me to reporduce your enviroment.

## License
Apache version 2.0
