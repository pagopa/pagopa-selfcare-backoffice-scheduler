console.log("Initializing pagopaBackoffice DB");
const brokerIbans = require('./usr/local/data/ibanDeletionRequest.json');

const mongoConnection = new Mongo();
const db = mongoConnection.getDB("pagopaBackoffice");

//add here collection initialization putting documents
db.getCollection('ibanDeletionRequest').insertMany(brokerIbans);

console.log("Initialization end");