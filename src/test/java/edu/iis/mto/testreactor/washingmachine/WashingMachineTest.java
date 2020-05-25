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

    @BeforeEach
    void setUp() throws Exception {
        washingMashine = new WashingMachine(dirtDetector, engine, waterPump);
    }
    //start - jedyna publiczna metoda pralki
    @Test void washingWithStaticProgramShouldFinishWithSuccess() {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.COTTON).withWeightKg(7).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(true).build();
        LaundryStatus status =  washingMashine.start(batch,program);
        assertTrue(LaundryStatus.builder().withRunnedProgram(Program.SHORT)
                                .withResult(Result.SUCCESS).withErrorCode(ErrorCode.NO_ERROR).build().equals(status));
    }

    @Test void washingWithStaticProgramShouldCallEngineAndPump() throws WaterPumpException, EngineException {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.COTTON).withWeightKg(7).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(true).build();
        washingMashine.start(batch,program);
        InOrder order = Mockito.inOrder(waterPump,engine);
        order.verify(waterPump).pour(7d);
        order.verify(engine).runWashing(Program.SHORT.getTimeInMinutes());
        order.verify(waterPump).release();
        order.verify(engine).spin();

    }

    @Test void washingWithTooHeavyLaundryBatchShouldFinishedWithFailure() {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.COTTON).withWeightKg(21d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(false).build();
        LaundryStatus status =  washingMashine.start(batch,program);
        assertTrue(LaundryStatus.builder().withRunnedProgram(null)
                                .withResult(Result.FAILURE).withErrorCode(ErrorCode.TOO_HEAVY).build().equals(status));
    }

    @Test void washingWithAutodetectionShouldFinishedWithSuccess() {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        when(dirtDetector.detectDirtDegree(batch)).thenReturn(new Percentage(40.0d));
        LaundryStatus status =  washingMashine.start(batch,program);
        assertTrue(LaundryStatus.builder().withRunnedProgram(Program.MEDIUM)
                                .withResult(Result.SUCCESS).withErrorCode(ErrorCode.NO_ERROR).build().equals(status));
    }

    @Test void washingWithAutodetectionShouldCallEngineWaterPumpAndDirtDetector() throws WaterPumpException, EngineException {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        when(dirtDetector.detectDirtDegree(batch)).thenReturn(new Percentage(60.0d));
        washingMashine.start(batch,program);
        InOrder order = Mockito.inOrder(dirtDetector,waterPump,engine);
        order.verify(dirtDetector).detectDirtDegree(batch);
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
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        doThrow(new IllegalArgumentException()).when(dirtDetector).detectDirtDegree(batch);
        LaundryStatus status =  washingMashine.start(batch,program);
        assertTrue(LaundryStatus.builder().withRunnedProgram(null)
                                 .withResult(Result.FAILURE).withErrorCode(ErrorCode.UNKNOWN_ERROR).build().equals(status));
    }

    @Test void washingWithIllegalArgEx_shouldCallDirtDetectorOneTime() {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        doThrow(new IllegalArgumentException()).when(dirtDetector).detectDirtDegree(batch);
        washingMashine.start(batch,program);
        verify(dirtDetector,times(1)).detectDirtDegree(batch);
    }


    @Test void washingWithEngineProblem_shouldFinishedWithEngineFailure() throws EngineException {
        LaundryBatch batch = LaundryBatch.builder().withMaterialType(Material.DELICATE).withWeightKg(2d).build();
        ProgramConfiguration program = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(true).build();
        //okreslenie zachowania zewnetrznej zaleznosci
        //silnik sie psuje przy wirowaniu -> rzucanie wyjatkow przez metode ktora zwraca void!!!!
        doThrow(new EngineException()).when(engine).spin();
        when(dirtDetector.detectDirtDegree(batch)).thenReturn(new Percentage(42.0d));
        LaundryStatus status =  washingMashine.start(batch,program);
        assertTrue(LaundryStatus.builder().withRunnedProgram(Program.MEDIUM)
                                .withResult(Result.FAILURE).withErrorCode(ErrorCode.ENGINE_FAILURE).build().equals(status));
    }
}
