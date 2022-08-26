package com.example.myfirstapp.backend.liveProcessing;

import android.util.Log;

import androidx.collection.CircularArray;

public class ModuleECG {

    private static ModuleECG mInstance = null;

    private float mRR;
    private float mRmsEcg;
    private float mRmsEmg;

    protected ModuleECG(){

    }


    public static synchronized ModuleECG getInstance() {
        if(null == mInstance){
            mInstance = new ModuleECG();
        }
        return mInstance;
    }


    public void doStuff(float rr, float rmsEcg, float rmsEmg){

        // do here some fancy processing

        // pass through
        this.mRR        = rr * 1000; // in ms
        this.mRmsEcg    = rmsEcg * 1000 * 1000; // in uV
        this.mRmsEmg    = rmsEmg * 1000 * 1000; // in uV

    }

    public float getRR()        { return this.mRR;      } //
    public float getRmsEcg()    { return this.mRmsEcg;  } //
    public float getRmsEmg()    { return this.mRmsEmg;  } //

}
