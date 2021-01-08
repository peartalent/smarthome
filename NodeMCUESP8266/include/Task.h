class Task
{
private:
    unsigned long workDuration;
    unsigned long storeWorkDuration;
    unsigned long elapsedTime;
    int deviceId;
    void (*callback)(int);
    void setWorkDuration(unsigned long duration);

public:
    Task(int id, unsigned long workDuration, void(callback)(int));
    ~Task();
    void update();
    void changeDuration(unsigned long newDuration);
    unsigned long getDuration();
    void pauseWork();
    void continueWork();
};
