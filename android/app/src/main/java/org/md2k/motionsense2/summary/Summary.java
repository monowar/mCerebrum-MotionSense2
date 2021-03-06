package org.md2k.motionsense2.summary;

import android.content.Context;

import org.md2k.mcerebrum.core.access.appinfo.AppInfo;
import org.md2k.motionsense2.ServiceMotionSense;
import org.md2k.motionsense2.device.Device;
import org.md2k.motionsense2.device.DeviceManager;
import org.md2k.motionsense2.device.SensorInfo;

import java.util.ArrayList;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Summary {
    private boolean isRunning;
    private long runningTime;
    private int configuredDevice;
    private ArrayList<DeviceSummary> devices;

    public static Summary getSummary(Context context){
        DeviceManager deviceManager = DeviceManager.getInstance();
        Summary summary = new Summary();
        summary.runningTime =  AppInfo.serviceRunningTime(context, ServiceMotionSense.class.getName());
        summary.isRunning = summary.runningTime >= 0;
        summary.configuredDevice = deviceManager.getDeviceNo();
        summary.devices = new ArrayList<>();
        for(int i=0;i<deviceManager.getDeviceNo();i++){
            Device d = deviceManager.getDevices().get(i);
            ArrayList<SensorInfo> s = d.getSensorInfo();
            summary.devices.add(new DeviceSummary(d.getDeviceInfo(), d.getSensorInfo()));
        }
        return summary;
    }

}
