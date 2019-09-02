# http2-undertow


**This application is NOT maintained anymore and might not work as expected anymore. This article was intended for Java 8 and Newer Java versions have a different HTTP/2 approach.**

Source code for blog article about JBoss undertow and HTTP/2 server push

https://vanwilgenburg.wordpress.com/2015/12/28/http2-server-push-with-jboss-undertow-and-how-to-monitor-things/


# run

run with `mvn clean package exec:exec`

Go to https://localhost:8443/hello-world/index.html

1、生成服务器端私钥kserver.keystore文件
keytool -genkeypair -alias serverkey -keyalg RSA -validity 365 -keysize 2048 -keystore kserver.keystore \
 -dname "C=CN,ST=SH,L=SH,O=gn,OU=iot,CN=localhost" -storepass storepwd -keypass storepwd

2、根据私钥，导出服务器端安全证书
keytool -export -alias serverkey -keystore kserver.keystore -rfc -file server_selfcert.cer -storepass storepwd 

3、将服务器端证书，导入到客户端的Trust KeyStore中
keytool -import -alias serverkey -file server_selfcert.cer  -keystore tclient.keystore -storepass storepwd 

4、生成客户端私钥kclient.keystore文件
keytool -genkey -alias clientkey -keyalg RSA -validity 365  -keysize 2048 -keystore kclient.keystore \
 -dname "C=CN,ST=SH,L=SH,O=gn,OU=iot,CN=localhost" -storepass storepwd -keypass storepwd


5、根据私钥，导出客户端安全证书
keytool -export -alias clientkey -keystore kclient.keystore -rfc -file client_selfcert.cer -storepass storepwd 

6、将客户端证书，导入到服务器端的Trust KeyStore中
keytool -import -alias clientkey -file client_selfcert.cer  -keystore tserver.keystore -storepass storepwd

7.将证书导入到 jdk/jre/lib/security/cacerts
导入证书命令
keytool -keystore cacerts -import -alias certificatekey -trustcacerts -file selfcert.cer -storepasswd changeit
查看是否导入成功
keytool -list -keystore cacerts -v


