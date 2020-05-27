package edu.iis.mto.testreactor.washingmachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WashingMachineTest {

    @Mock
    private DirtDetector dirtDetector;
    @Mock
    private Engine engine;
    @Mock
    private WaterPump waterPump;
    private WashingMachine washingMashine;
    private LaundryBatch laundryBatch;
    private ProgramConfiguration programConfiguration;
    private LaundryStatus expectedLaundryStatus;
    @BeforeEach
    void setUp() throws Exception {
        washingMashine = new WashingMachine(dirtDetector, engine, waterPump);
    }

    private void createLaundryBatch(Material material, double overweight) {
        laundryBatch =  LaundryBatch.builder().withMaterialType(material).withWeightKg(overweight).build();
    }

    private void createConfigurationProgram(Program program, boolean spin) {
        programConfiguration = ProgramConfiguration.builder().withProgram(program).withSpin(spin).build();
    }

    private void generateExpectedResultStatus(Program program, Result result, ErrorCode errorCode) {
        expectedLaundryStatus = LaundryStatus.builder().withRunnedProgram(program).withResult(result).withErrorCode(errorCode).build();
    }

    //start - jedyna publiczna metoda pralki
    @Test void washingWithStaticProgramShouldFinishWithSuccess() {
        createLaundryBatch(Material.COTTON, 7d);
        createConfigurationProgram(Program.SHORT, true);
        LaundryStatus status =  washingMashine.start(laundryBatch,programConfiguration);
        generateExpectedResultStatus(Program.SHORT, Result.SUCCESS, ErrorCode.NO_ERROR);
        assertEquals(expectedLaundryStatus, status);
    }

    @Test void washingWithStaticProgramShouldCallEngineAndPump() throws WaterPumpException, EngineException {
        createLaundryBatch(Material.COTTON, 7d);
        createConfigurationProgram(Program.SHORT, true);
        washingMashine.start(laundryBatch,programConfiguration);
        InOrder order = Mockito.inOrder(waterPump,engine);
        order.verify(waterPump).pour(7d);
        order.verify(engine).runWashing(Program.SHORT.getTimeInMinutes());
        order.verify(waterPump).release();
        order.verify(engine).spin();
    }

    @Test void washingWithTooHeavyLaundryBatchShouldFinishedWithFailure() {
        createLaundryBatch(Material.COTTON, 21d);
        createConfigurationProgram(Program.SHORT, false);
        LaundryStatus status =  washingMashine.start(laundryBatch,programConfiguration);
        generateExpectedResultStatus(null, Result.FAILURE, ErrorCode.TOO_HEAVY);
        assertEquals(expectedLaundryStatus,status);
        //assertTrue(expectedLaundryStatus.equals(status));
    }

    @Test void washingWithAutodetectionShouldFinishedWithSuccess() {
        createLaundryBatch(Material.DELICATE, 2d);
        createConfigurationProgram(Program.AUTODETECT, false);
        //okreslenie zachowania zewnetrznej zaleznosci
        when(dirtDetector.detectDirtDegree(laundryBatch)).thenReturn(new Percentage(40.0d));
        LaundryStatus status =  washingMashine.start(laundryBatch,programConfiguration);
        generateExpectedResultStatus(Program.MEDIUM, Result.SUCCESS, ErrorCode.NO_ERROR);
        assertEquals(expectedLaundryStatus,status);
    }

    @Test void washingWithAutodetectionShouldCallEngineWaterPumpAndDirtDetector() throws WaterPumpException, EngineException {
        createLaundryBatch(Material.DELICATE, 2d);
        createConfigurationProgram(Program.AUTODETECT, false);
        //okreslenie zachowania zewnetrznej zaleznosci
        when(dirtDetector.detectDirtDegree(laundryBatch)).thenReturn(new Percentage(60.0d));
        washingMashine.start(laundryBatch,programConfiguration);
        InOrder order = Mockito.inOrder(dirtDetector,waterPump,engine);
        order.verify(dirtDetector).detectDirtDegree(laundryBatch);
        order.verify(waterPump).pour(2d);
        order.verify(engine).runWashing(Program.LONG.getTimeInMinutes());
        order.verify(waterPump).release();
    }

    /*@Test void washingWithAutodetectionAndDirtyPercentageOutOuRange_shouldThrowIllegalArgEx() {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        when(dirtDetector.detectDirtDegree(batch)).thenReturn(new Percentage(140.0d));
        doThrow(new IllegalArgumentException()).when(dirtDetector).detectDirtDegree(batch);
        LaundryStatus status =  washingMashine.start(batch,program);
        assertThrows(IllegalArgumentException.class,()-> new Percentage(140.0d));
        assertTrue(LaundryStatus.builder().withRunnedProgram(Program.MEDIUM)
                                .withResult(Result.SUCCESS).withErrorCode(ErrorCode.NO_ERROR).build().equals(status));
    }*/

    @Test void washingWithIllegalArgEx_shouldFinishedWithFailure() {
        createLaundryBatch(Material.DELICATE, 2d);
        createConfigurationProgram(Program.AUTODETECT, false);
        //okreslenie zachowania zewnetrznej zaleznosci
        doThrow(new IllegalArgumentException()).when(dirtDetector).detectDirtDegree(laundryBatch);
        LaundryStatus status =  washingMashine.start(laundryBatch,programConfiguration);
        generateExpectedResultStatus(null, Result.FAILURE, ErrorCode.UNKNOWN_ERROR);
        assertEquals(expectedLaundryStatus,status);
    }

    @Test void washingWithIllegalArgEx_shouldCallDirtDetectorOneTime() {
        createLaundryBatch(Material.DELICATE, 2d);
        createConfigurationProgram(Program.AUTODETECT, false);
        //okreslenie zachowania zewnetrznej zaleznosci
        doThrow(new IllegalArgumentException()).when(dirtDetector).detectDirtDegree(laundryBatch);
        washingMashine.start(laundryBatch,programConfiguration);
        verify(dirtDetector,times(1)).detectDirtDegree(laundryBatch);
    }


    @Test void washingWithEngineProblem_shouldFinishedWithEngineFailure() throws EngineException {
        createLaundryBatch(Material.DELICATE, 2d);
        createConfigurationProgram(Program.AUTODETECT, true);
        //okreslenie zachowania zewnetrznej zaleznosci
        //silnik sie psuje przy wirowaniu -> rzucanie wyjatkow przez metode ktora zwraca void!!!!
        doThrow(new EngineException()).when(engine).spin();
        when(dirtDetector.detectDirtDegree(laundryBatch)).thenReturn(new Percentage(42.0d));
        LaundryStatus status =  washingMashine.start(laundryBatch,programConfiguration);
        generateExpectedResultStatus(Program.MEDIUM, Result.FAILURE, ErrorCode.ENGINE_FAILURE);
        assertEquals(expectedLaundryStatus,status);
    }
}
