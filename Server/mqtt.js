const mqtt = require('mqtt');
const endClient = require('./Model/end-client-layer.js')
const client = mqtt.connect('mqtt://broker.hivemq.com')
const express = require('express')
var mysql = require('mysql')
// const initAPIs = require("./routes/api");
const app = express()
const PORT = process.env.PORT || 3000;
var requestMessage = 0, statusMessageQueue = {};
const THIEF = 113;
const AIR = 114;
const ADDCARD = 110;

var queueTopic=0;
var tmpName="";
var lastWarn=0;
var delayTime=15;
//connect to db
var con = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "smart_home"
});

con.connect(function (err) {
  if (err) throw err;
  console.log("Connected!!!");
});

//-------------------------------------------------------------------------------------------
function sendMessage(topic, message) {
  if (message.assignList !== undefined) {
    for (i = 0; i < message.assignList.length; i++) {
      message.assignList[i].mssid = requestMessage;
      statusMessageQueue[requestMessage] = { resp_type: "Not recv", mss: "" };
      requestMessage++;
    }
  }
  else {
    message.mssid = requestMessage;
    statusMessageQueue[requestMessage] = { resp_type: "Not recv", mss: "" };
    requestMessage++;
  }
  client.publish(topic, JSON.stringify(message));
}

var deviceDict = {};
var defaultDevice = { //danh sach cac thiet bi cai dat
  "assignList": [
    {
      "mssid": 1,
      "id": 1, //Manage by server
      "type": 1, //ex LED 1, AIR 2, NFC 3
      "pin": 5
    },
    {
      "mssid": 2,
      "id": 69,
      "type": 2,
      "pin": 17
    },
    {
      "mssid": 3,
      "id": 96,
      "type": 3,
      "pin": 2
    }
  ],
  "permissions": [  //danh sach cac the co the mo cua
    "4324234324",
    "10043242342340",
    "4234324234"
  ]
}

for (i = 0; i < defaultDevice.assignList.length; i++) {
  // console.log(defaultDevice.assignList[i].id);
  deviceDict[defaultDevice.assignList[i].id] = [];
}
client.on('connect', () => {
  client.subscribe('sownInfo')
  client.subscribe('sownAssign')
  client.subscribe('sownResponse')
})
client.on("message", (topic, message) => {
  try {
    if (topic == 'sownInfo') {
      var obj = JSON.parse(message);
      check = false;
      for (var key in deviceDict) {
        if (key == obj.id) {
          check = true;
          break;
        }
      }
      if (check == true) {
        deviceDict[obj.id].push(obj);
        //luu vao database
        con.query("SELECT COUNT(*) AS num FROM device_info WHERE device_id = " + obj.id, function (err, result) {
          if (err) throw err;
          num = result[0].num;
          if(num>43200){
            con.query("DELETE FROM `device` WHERE device_id = " + obj.id + " AND time_stamp = (SELECT MIN(time_stamp) FROM `device_info`)", function (err, result) {
              if (err) throw err;
              // console.log("Delete 1 row success!")
            });
          }
        });

        var sql = "SELECT type FROM device WHERE id = " + obj.id;
        var type;
        con.query(sql, function (err, result, type) {
          if (err) throw err;
          obj.type = result[0].type;

          if(obj.type == 1){                                //gui json LED
            sql = "UPDATE `device_info` SET `value`="+ obj.value +" WHERE device_id = " + obj.id;
            sendMessage("IoT8_HUST", obj);
          }
          else if(obj.type == 2){                           //gui json AIR
            if(obj.value > 50&&(Date.now()-delayTime>lastWarn)){
              lastWarn=Date.now();
              warning = {};
              warning.code = AIR;
              warning.message = "warning AIR";
              warning.id = obj.id;
              warning.value = obj.value;
              sendMessage("IoT8_HUST_WARNING", warning);
              // console.log(warning);
            }
            
            sql = "INSERT INTO `device_info` (device_id, value, time_stamp) VALUES (" + obj.id + "," + obj.value + "," +obj.time_stamp + ");"
            sendMessage("IoT8_HUST", obj);
          }
          else{                                         //gui json NFC
            if(obj.permission == "denied"){
              
              warning = {};
              warning.code = THIEF;
              warning.message = "warning THIEF";
              sendMessage("IoT8_HUST_WARNING", warning);
            }

            
            sql = "INSERT INTO `device_info` (device_id, time_stamp, permission) VALUES (" + obj.id + "," + Date.now() + "," +"'"+ obj.permission +"'"+");"
            sendMessage("IoT8_HUST", obj);
          }
          con.query(sql, function (err, result) {
            if (err) throw err;
            // console.log("Insert success!")
          });
        });
      }
      else
        console.log("Not assign id ");
    }
    if (topic == 'sownAssign') {
      var obj = JSON.parse(message);
      if(obj.requestAssign==true){
      client.publish(topic, JSON.stringify(defaultDevice));}
    }
    if (topic == 'sownResponse') {
      console.log(message);
      var message = JSON.parse(message);
       console.log(queueTopic);
      statusMessageQueue[message.mssid] = { resp_type: message.resp_type, mss: message.mss };
      if(message.mssid==queueTopic){
        queueTopic=0;
        if(message.mss=="400"){
          warning = {};

          warning.code = ADDCARD;
          warning.message = "failed";
          sendMessage("IoT8_HUST_WARNING", warning);
        }else{
          //lấy bién tmpname ra dùng
          //upload len database
          var sql = "INSERT INTO `permission`(`device_id`, `codeCard`, `nameCard`) VALUES (96,'"+message.mss+"',"+"'"+tmpName+"')";
          console.log(sql);
          con.query(sql, function (err, result) {
            if (err) throw err;
          });
          defaultDevice.permissions.push(message.mss);
          warning = {};
          warning.code = ADDCARD;
          warning.message = "success";
          warning.name = tmpName;
          sendMessage("IoT8_HUST_WARNING", warning);
        }
        tmpName="";
      }
    }
  }
  catch (e) {
    console.log(e);
  }
})

//API

//bat/tat bong den
app.get('/turnOnOffBulb', (req, res) => {
  // console.log(req.query.id);
  id = req.query.id;
  value = req.query.value;
  sendMessage("sownCmd",{
    "id": id,
    "cmd": value, 
  })
});

//them the tu
app.get('/addRFID', (req, res) => {
  nameRFID = req.query.name;
  var checkAvailableNameCard=true;
  var sql = "SELECT nameCard FROM `permission`";
  con.query(sql, function (err, result) {
    if (err) throw err;
    for(var i=0; i<result.length ; i++){
      if(nameRFID == result[i].nameCard){
        checkAvailableNameCard=false;
        break;
      }
    }
    res.send(checkAvailableNameCard);
  });
  if(checkAvailableNameCard==true){
    con.query(sql, function (err, result) {
      if (err) throw err;
    });
    queueTopic=requestMessage;
    tmpName=nameRFID;
    sendMessage("sownCmd",{
      "id": 96,
      "cmd": 1, 
    })
  }
});

//xoa the tu
app.get('/deleteCard', (req, res) => {
  id = req.query.id;
  var sql = "SELECT `id`, `nameCard` FROM `permission`";
  con.query(sql, function (err, result) {
    if (err) throw err;
    res.send(result);
  });
  sql = "SELECT `codeCard` FROM `permission` WHERE id="+id;
  var cardName;
  con.query(sql, function (err, result) {
    if (err) throw err;
    cardName=result[0];
    sendMessage("sownCmd",{
      "id": 96,
      "cmd": 3,
      "value": cardName.codeCard
    })
  });
  sql = "DELETE FROM permission WHERE id=" + id;
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
  });
});

//check đăng nhập
app.get('/login', (req, res) => {
  username = req.query.username;
  password = req.query.password;
  var sql = "SELECT username, password FROM `account`";
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
    // console.log("res = "+result);
    var r=false;
    for(var i=0 ; i < result.length ; i++){
      if(username == result[i].username){
        if(password == result[i].password){
          r = true;
          break;
        }
      }
    }
    // console.log(r);
    res.send(r);
  });
});

app.get('/getDevice', (req, res) => {
  id = req.query.id
  return res.send(JSON.stringify(deviceDict[id]));
});

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}.`);
});

//lay trang thai hien thai cua bong den
app.get('/getStateBulbs', (req, res) => {
  // id = req.query.id;
  var sql = "SELECT device_id, value FROM `device_info`, `device` WHERE device.id = device_info.device_id AND device.type = 1";
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
    // console.log("res = "+result);
    res.send(result);
  });
});

//lay lich su ra vao cua Cua tu
app.get('/getEntranceHistory', (req, res) => {
  id = req.query.id;
  // value =
  var sql = "SELECT time_stamp, permission FROM `device_info` WHERE device_id = " + id;
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
    // console.log("res = "+result);
    res.send(result);
  });
});

//lay chi so khong khi theo thoi gian (lich su)
app.get('/getAirQuality', (req, res) => {
  var sql = "SELECT time_stamp, value FROM `device_info` WHERE device_id = 69";
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
    // console.log("res = "+result);
    res.send(result);
  });
});

//lay danh sach the
app.get('/getCardList', (req, res) => {
  var sql = "SELECT `id`, `nameCard` FROM `permission`";
  con.query(sql, function (err, result) {
    if (err) throw err;
    res.send(result);
  });
});

//lay so do khong khi theo gio
// app.get('/getAirPerHour', (req, res) => {
//   var sql = "SELECT time_stamp, value FROM `device_info` WHERE device_id=69 AND ";
//   con.query(sql, function (err, result) {
//     if (err) throw err;
//     res.send(result);
//   });
// });

//lay danh sach bong den
app.get('/getListBulb', (req, res) => {
  // id = req.query.id;
  var sql = "SELECT device_id, value FROM `device_info`, `device` WHERE device.id = device_info.device_id AND device.type = 1";
  // console.log(sql);
  con.query(sql, function (err, result) {
    if (err) throw err;
    // console.log("res = "+result);
    res.send(result);
  });
});