package kodiForm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 * Created by mike on 03.12.15.
 */
public class DisabledSelectionModel<T> extends MultipleSelectionModel<T> {
    DisabledSelectionModel() {
        super.setSelectedIndex(-1);
        super.setSelectedItem(null);
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return FXCollections.<Integer>emptyObservableList();
    }

    @Override
    public ObservableList<T> getSelectedItems() {
        return FXCollections.<T>emptyObservableList();
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void selectFirst() {
    }

    @Override
    public void selectIndices(int index, int... indicies) {
    }

    @Override
    public void selectLast() {
    }

    @Override
    public void clearAndSelect(int index) {
    }

    @Override
    public void clearSelection() {
    }

    @Override
    public void clearSelection(int index) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isSelected(int index) {
        return false;
    }

    @Override
    public void select(int index) {
    }

    @Override
    public void select(T item) {
    }

    @Override
    public void selectNext() {
    }

    @Override
    public void selectPrevious() {
    }

}

