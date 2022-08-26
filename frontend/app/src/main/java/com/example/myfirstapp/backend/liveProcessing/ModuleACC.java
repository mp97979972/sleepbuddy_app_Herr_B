package com.example.myfirstapp.backend.liveProcessing;

import android.util.Log;

import androidx.collection.CircularArray;

/* ACC Axis

    Z-Axis : Axis through bottom (electrode side) and lid of the Sleepbuddy. ~ +1, if electrode side is directed downward
    X-Axis : Axis through the long side of the Sleepbuddy (through the side with the button). ~ +1, if button is directed downward
    Y-Axis : Axis through the short side of the Sleepbuddy (just the remaining side). ~ +1, if blinking LED is directed downward

*/
public class ModuleACC {

    private static ModuleACC mInstance = null;

    private float mAccX;
    private float mAccY;
    private float mAccZ;
    private float mRmsSnore;
    private float mRmsMove1;
    private float mRmsMove2;

    public float getAccX()      { return this.mAccX;        } //
    public float getAccY()      { return this.mAccY;        } //
    public float getAccZ()      { return this.mAccZ;        } //
    public float getRmsSnore()  { return this.mRmsSnore;    } //
    public float getRmsMove1()  { return this.mRmsMove1;    } //
    public float getRmsMove2()  { return this.mRmsMove2;    } //

    protected ModuleACC(){

    }


    public static synchronized ModuleACC getInstance() {
        if(null == mInstance){
            mInstance = new ModuleACC();
        }
        return mInstance;
    }



    public void doStuff(float accX, float accY, float accZ, float rmsSnore, float rmsMove1, float rmsMove2){

        // do here some fancy processing

        // pass through with physical transformation (if the calculation is correct, this should be implement before queue filling)


        this.mAccX = accX; // in g
        this.mAccY = accY; // in g
        this.mAccZ = accZ; // in g
        this.mRmsSnore = rmsSnore * 1000; // in mg
        this.mRmsMove1 = rmsMove1 * 1000; // in mg
        this.mRmsMove2 = rmsMove2 * 1000; // in mg
    }


}
