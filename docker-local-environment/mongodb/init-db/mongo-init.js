console.log("Initializing pagopaBackoffice DB");
const brokerIbans = require('./usr/local/data/scheduledTasks.json');

const mongoConnection = new Mongo();
const db = mongoConnection.getDB("pagopaBackoffice");

//add here collection initialization putting documents
db.getCollection('scheduledTasks').insertMany(brokerIbans);

console.log("Initialization end");