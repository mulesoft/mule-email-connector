FROM antespi/docker-imap-devel

RUN apt-get upgrade \
&& apt-get install openssl \
&& mkdir /etc/postfix/ssl \
&& cd /etc/postfix/ssl \
&& openssl genrsa -des3 -passout pass:p4ssw0rd -out server.pass.key 2048 \
&& openssl rsa -passin pass:p4ssw0rd -in server.pass.key -out server.key \
&& rm server.pass.key \
&& openssl req -new -key server.key -out server.csr -subj "/C=AR/ST=CABA/L=Puerto Madero/O=Mulesoft/OU=SDK/CN=mulesoft.test/emailAddress=juan.desimoni@mulesoft.test" \
&& openssl x509 -req -days 9999 -in server.csr -signkey server.key -out server.crt \
&& openssl req -new -passout pass:p4ssw0rd -x509 -extensions v3_ca -keyout cakey.pem -out cacert.pem -days 9999 -subj "/C=AR/ST=CABA/L=Puerto Madero/O=Mulesoft/OU=SDK/CN=mulesoft.test/emailAddress=juan.desimoni@mulesoft.test" \
&& ls \
&& chmod 600 server.key \
&& chmod 600 cakey.pem \
&& postconf -e 'smtpd_use_tls = yes' \
&& postconf -e 'smtpd_tls_auth_only = no' \
&& postconf -e 'smtpd_tls_key_file = /etc/postfix/ssl/server.key' \
&& postconf -e 'smtpd_tls_cert_file = /etc/postfix/ssl/server.crt' \
&& postconf -e 'smtpd_tls_CAfile = /etc/postfix/ssl/cacert.pem' \
&& postconf -e 'tls_random_source = dev:/dev/urandom' \
&& postconf -e 'myhostname = mulesoft.test'