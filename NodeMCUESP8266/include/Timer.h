class Timer
{
private:
    unsigned long beforeTime;
    unsigned long currentTime;
    static Timer *instance;
    Timer(/* args */);
    ~Timer();

public:
    static Timer *getInstance();
    //Init first loop
    void initialize();
    //Update duration of loop
    void update();
    //Duration of the last loop
    unsigned long delta();
    //New loop
    void reset();
};