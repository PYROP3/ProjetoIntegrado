package com.street.analyzer.record;

import com.street.analyzer.ConstantsTest;
import com.street.analyzer.utils.Constants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ValuesTest {

    private Values mValues;

    @Before
    public void setup(){
        mValues = new Values();
    }

    @Test
    public void testX(){
        ArrayList<Float> x = new ArrayList<>();
        x.add(ConstantsTest.X_VALUE_1);
        x.add(ConstantsTest.X_VALUE_2);
        x.add(ConstantsTest.X_VALUE_3);

        mValues.addAllXValues(x);

        assertTrue("testX", mValues.getXValue().get(0) == ConstantsTest.X_VALUE_1);
        assertTrue("testX", mValues.getXValue().get(1) == ConstantsTest.X_VALUE_2);
        assertTrue("testX", mValues.getXValue().get(2) == ConstantsTest.X_VALUE_3);

    }
}