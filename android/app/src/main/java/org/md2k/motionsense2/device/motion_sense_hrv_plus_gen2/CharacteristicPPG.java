package org.md2k.motionsense2.device.motion_sense_hrv_plus_gen2;
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

import com.polidea.rxandroidble2.RxBleConnection;

import org.md2k.datakitapi.time.DateTime;
import org.md2k.motionsense2.configuration.ConfigDevice;
import org.md2k.motionsense2.configuration.ConfigSensor;
import org.md2k.motionsense2.device.Characteristics;
import org.md2k.motionsense2.device.Data;
import org.md2k.motionsense2.device.Sensor;
import org.md2k.motionsense2.device.SensorInfo;

import java.util.ArrayList;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class CharacteristicPPG extends Characteristics {
    private static final UUID CHARACTERISTICS = UUID.fromString("DA39C925-1D81-48E2-9C68-D0AE4BBD351F");
    private static final int MAX_SEQUENCE_NUMBER = 65536;
    private int lastSequenceNumber = -1;
    private long lastCorrectedTimestamp = -1;
    private double[] ppgLast = new double[0];
    private double[] sequenceNumberLast = new double[0];
    private double[] rawLast = new double[0];
    private static final int PPG = Sensor.PPG.getId();
    private static final int SEQUENCE_NUMBER_PPG = Sensor.SEQUENCE_NUMBER_PPG.getId();
    private static final int RAW_PPG = Sensor.RAW_PPG.getId();
    CharacteristicPPG() {
        super(new Sensor[]{Sensor.PPG, Sensor.SEQUENCE_NUMBER_PPG, Sensor.RAW_PPG});
    }

    @Override
    public boolean isEnable(ConfigDevice configDevice) {
        if (configDevice.getSensor(Sensor.PPG.getDataSourceType()).isEnable()) return true;
        return false;
    }

    @Override
    public ArrayList<SensorInfo> getSensorInfo(ConfigDevice configDevice) {
        super.setSensorInfos();
        boolean enable = isEnable(configDevice);
        boolean raw = configDevice.isSaveRaw();
        getSensorInfo(PPG).setEnable(enable);
        getSensorInfo(SEQUENCE_NUMBER_PPG).setEnable(enable && raw);
        getSensorInfo(RAW_PPG).setEnable(enable && raw);
        return super.getSensorInfos();
    }

    @Override
    public Observable<Data> listen(RxBleConnection rxBleConnection, ConfigDevice configDevice) {
        ConfigSensor cPPG = configDevice.getSensor(Sensor.PPG.getDataSourceType());
        boolean isSaveRaw = configDevice.isSaveRaw();
        lastSequenceNumber = -1;
        lastCorrectedTimestamp = -1;
        return getCharacteristicListener(rxBleConnection, CHARACTERISTICS)
                .flatMap(new Function<byte[], Observable<Data>>() {
                    @Override
                    public Observable<Data> apply(byte[] bytes) throws Exception {
                        long curTime = DateTime.getDateTime();
                        ArrayList<Data> data = new ArrayList<>();
                        ppgLast = getPPG(bytes);
                        rawLast = getRaw(bytes);
                        int sequenceNumber = getSequenceNumber(bytes);
                        sequenceNumberLast = new double[]{sequenceNumber};
                        long correctedTimestamp = correctTimeStamp(sequenceNumber, curTime, lastSequenceNumber, lastCorrectedTimestamp, cPPG.getFrequency(), MAX_SEQUENCE_NUMBER);
                        if (cPPG.isEnable()) {
                            data.add(new Data(PPG, correctedTimestamp, ppgLast));
                        }
                        if (isSaveRaw) {
                            data.add(new Data(RAW_PPG, correctedTimestamp, rawLast));
                            data.add(new Data(SEQUENCE_NUMBER_PPG, correctedTimestamp, sequenceNumberLast));
                        }
                        lastCorrectedTimestamp = correctedTimestamp;
                        lastSequenceNumber = sequenceNumber;
                        Data[] d = new Data[data.size()];
                        data.toArray(d);
                        return Observable.fromArray(d);
                    }
                });
    }

    /**
     * Infra-red 1: bytes 13-11
     * Infra-red 2: bytes 10-8
     * Green/Red 1: bytes 7-5
     * Green/Red 2: bytes 4-2
     * Counter: bytes 1-0
     */
    private double[] getPPG(byte[] bytes) {
        double[] sample = new double[4];
        sample[0] = convertPPGValues(bytes[0], bytes[1], bytes[2]);
        sample[1] = convertPPGValues(bytes[3], bytes[4], bytes[5]);
        sample[2] = convertPPGValues(bytes[6], bytes[7], bytes[8]);
        sample[3] = convertPPGValues(bytes[9], bytes[10], bytes[11]);
        return sample;
    }

    /**
     * each ppg dc value is of type 32-bit single-precision float sent over the channels in an
     * unsigned uint8 array, floatcast of size 4. The format is little endian. So, for example, in
     * Channel Infra-red floatcast[0] corresponds to the MSB and floatcast[3] is the LSB. The
     * counter is also in little-endian form
     */
    private static int convertPPGValues(byte intcast2, byte intcast1, byte intcast0) {
        return (int) (intcast2 << 16 | intcast1 << 8 | intcast0); //TODO: This needs testing.
    }


    private int getSequenceNumber(byte[] data) {
        return ((data[data.length - 2] & 0xff) << 8) | (data[data.length - 1] & 0xff);
    }

    private double[] getRaw(byte[] bytes) {
        double[] sample = new double[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            sample[i] = bytes[i];
        return sample;
    }
}
