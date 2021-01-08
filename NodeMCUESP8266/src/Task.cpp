#include "Task.h"
#include "Timer.h"
#include <Arduino.h>

void Task::setWorkDuration(unsigned long duration)
{
    workDuration = duration;
    storeWorkDuration = workDuration;
}

Task::Task(int id, unsigned long duration, void(callback)(int))
{
    deviceId = id;
    setWorkDuration(duration);
    elapsedTime = 0;
    this->callback = callback;
}
Task::~Task()
{
    deviceId = 0;
    workDuration = 0;
    elapsedTime = 0;
    callback = NULL;
}
void Task::update()
{
    if (workDuration != INFINITY)
    {
        elapsedTime += Timer::getInstance()->delta();
        if (elapsedTime >= workDuration)
        {
            elapsedTime -= workDuration;
            if (callback != NULL)
            {
                callback(deviceId);
            }
        }
    }
}
void Task::changeDuration(unsigned long newDuration)
{
    setWorkDuration(newDuration);
}
unsigned long Task::getDuration()
{
    return storeWorkDuration;
}
void Task::pauseWork()
{
    workDuration = INFINITY;
}
void Task::continueWork()
{
    workDuration = storeWorkDuration;
}