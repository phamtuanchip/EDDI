package ai.labs.property.impl;

import ai.labs.memory.Data;
import ai.labs.memory.IConversationMemory;
import ai.labs.memory.IData;
import ai.labs.memory.IDataFactory;
import ai.labs.property.IPropertyDisposer;
import ai.labs.property.model.PropertyEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author ginccc
 */
public class PropertyDisposerTaskTest {
    private static final String KEY_EXPRESSIONS_PARSED = "expressions:parsed";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_INPUT_INITIAL = "input:initial";
    private PropertyDisposerTask propertyDisposerTask;
    private IConversationMemory conversationMemory;
    private IConversationMemory.IWritableConversationStep currentStep;
    private IConversationMemory.IConversationStep previousStep;
    private IPropertyDisposer propertyDisposer;
    private IDataFactory dataFactory;

    @Before
    public void setUp() throws Exception {
        propertyDisposer = mock(IPropertyDisposer.class);
        dataFactory = mock(IDataFactory.class);
        conversationMemory = mock(IConversationMemory.class);
        currentStep = mock(IConversationMemory.IWritableConversationStep.class);
        IConversationMemory.IConversationStepStack previousConversationSteps = mock(IConversationMemory.IConversationStepStack.class);
        previousStep = mock(IConversationMemory.IConversationStep.class);
        when(previousConversationSteps.get(eq(0))).thenAnswer(invocation -> previousStep);
        when(conversationMemory.getCurrentStep()).thenAnswer(invocation -> currentStep);
        when(conversationMemory.getPreviousSteps()).thenAnswer(invocation -> previousConversationSteps);
        propertyDisposerTask = new PropertyDisposerTask(propertyDisposer, dataFactory);
    }

    @Test
    public void executeTask() throws Exception {
        //setup
        final String userInput = "Some Input From the User";
        List<PropertyEntry> propertyEntries = new LinkedList<>();
        propertyEntries.add(new PropertyEntry(Collections.singletonList("someMeaning"), "someValue"));
        propertyEntries.add(new PropertyEntry(Collections.singletonList("user_input"), userInput));
        IData<List<PropertyEntry>> expectedPropertyData = new Data<>("properties:extracted", propertyEntries);

        final String propertyExpression = "property(someMeaning(someValue))";
        when(currentStep.getLatestData(eq(KEY_EXPRESSIONS_PARSED))).thenAnswer(invocation ->
                new Data<>(KEY_EXPRESSIONS_PARSED, propertyExpression));
        when(propertyDisposer.extractProperties(eq(propertyExpression))).thenAnswer(invocation -> {
            List<PropertyEntry> ret = new LinkedList<>();
            ret.add(new PropertyEntry(Collections.singletonList("someMeaning"), "someValue"));
            return ret;
        });
        when(previousStep.getLatestData(eq(KEY_ACTIONS))).thenAnswer(invocation ->
                new Data<>(KEY_ACTIONS, Arrays.asList("CATCH_ANY_INPUT_AS_PROPERTY", "someOtherAction")));
        when(previousStep.getLatestData(eq(KEY_INPUT_INITIAL))).thenAnswer(invocation ->
                new Data<>(KEY_INPUT_INITIAL, userInput));
        when(dataFactory.createData(eq("properties:extracted"), any(List.class), eq(true))).
                thenAnswer(invocation -> {
                    Data<List<PropertyEntry>> ret = new Data<>("properties:extracted", propertyEntries);
                    ret.setPublic(true);
                    return ret;
                });

        //test
        propertyDisposerTask.executeTask(conversationMemory);

        //assert
        verify(currentStep, times(1)).getLatestData(KEY_EXPRESSIONS_PARSED);
        verify(previousStep, times(1)).getLatestData(KEY_ACTIONS);
        verify(previousStep, times(1)).getLatestData(KEY_INPUT_INITIAL);
        verify(currentStep).storeData(expectedPropertyData);
    }

}