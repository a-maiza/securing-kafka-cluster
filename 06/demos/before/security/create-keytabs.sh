kadmin.local -q 'addprinc -randkey kafka/broker-1.m5_default@PLURALSIGHT.COM'
kadmin.local -q "ktadd -k /etc/security/keytabs/broker1.keytab kafka/broker-1.m5_default@PLURALSIGHT.COM"

kadmin.local -q 'addprinc -randkey kafka/broker-2.m5_default@PLURALSIGHT.COM'
kadmin.local -q "ktadd -k /etc/security/keytabs/broker2.keytab kafka/broker-2.m5_default@PLURALSIGHT.COM"

kadmin.local -q 'addprinc -randkey kafka/broker-3.m5_default@PLURALSIGHT.COM'
kadmin.local -q "ktadd -k /etc/security/keytabs/broker3.keytab kafka/broker-3.m5_default@PLURALSIGHT.COM"

kadmin.local -q 'addprinc -randkey producer@PLURALSIGHT.COM'
kadmin.local -q "ktadd -k /etc/security/keytabs/producer.keytab producer@PLURALSIGHT.COM"

kadmin.local -q 'addprinc -randkey consumer@PLURALSIGHT.COM'
kadmin.local -q "ktadd -k /etc/security/keytabs/consumer.keytab consumer@PLURALSIGHT.COM"