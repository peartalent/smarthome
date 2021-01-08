var fs=require('fs');
const DoorState = require('../Entity/door_state');
const dateFormat = require('dateformat');
var ds=require('../Entity/door_state')
function saveLogDoor(doorLog){
    var fileName='logs/'+dateFormat(new Date(), "ddmmyy" ).toString()+'.json';
    console.log(fileName);
    try {
        if(fs.existsSync(fileName)){
            // add doorlog after currentlingin file
        }else{
            var data = JSON.stringify(doorLog);
            data='['+data+']';
            fs.writeFileSync(fileName, data);
           
        }
    } catch (error) {
        console.log(error);
    }
   
}

saveLogDoor(new DoorState("11-11-11",1,11111));