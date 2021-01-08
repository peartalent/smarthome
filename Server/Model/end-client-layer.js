function warnPublic(device_id,device_type,option_value){
    //1 is about gas 2 is about door
    var warnObj={};
    if(device_type==1){
        warnObj.warningGas={id:device_id,state:option_value};
    }
    if(device_type==2){
        warnObj.warningRFID={id:device_id,state:option_value};
    }
    return warnObj;
}
module.exports={
    warnPublic
}