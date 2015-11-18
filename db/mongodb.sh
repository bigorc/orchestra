#Add authentication for Mongodb 2.4
mongo --eval ' db = db.getSiblingDB("orchestra");
db.addUser({user: "orchestra", pwd: "orchestra",
            roles: [ "readWrite", "dbAdmin" ]})'
