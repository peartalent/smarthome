#include "Timer.h"
#include "Arduino.h"

Timer *Timer::instance = nullptr;

Timer *Timer::getInstance()
{
    if (Timer::instance == nullptr)
        Timer::instance = new Timer();
    return Timer::instance;
}
void Timer::initialize()
{
    beforeTime = millis();
}
void Timer::update()
{
    currentTime = millis();
}
unsigned long Timer::delta()
{
    return currentTime - beforeTime;
}
void Timer::reset()
{
    beforeTime = currentTime;
}
Timer::Timer(/* args */)
{
}

Timer::~Timer()
{
}
