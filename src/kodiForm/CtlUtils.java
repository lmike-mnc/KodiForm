package kodiForm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

/**
 * Created by mike on 03.12.15.
 */
public class CtlUtils {
    private static NullTableViewSelectionModel nullSelection = null;
    public static NullTableViewSelectionModel disabledSelection(TableView ctl) {
        if (nullSelection == null) nullSelection = new NullTableViewSelectionModel(ctl);
        return nullSelection;
    }

    private static class NullTableViewSelectionModel extends TableView.TableViewSelectionModel {
        public NullTableViewSelectionModel(TableView tableView) {
            super(tableView);
        }

        @Override
        public ObservableList<TablePosition> getSelectedCells() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public void selectLeftCell() {

        }

        @Override
        public void selectRightCell() {

        }

        @Override
        public void selectAboveCell() {

        }

        @Override
        public void selectBelowCell() {

        }

        @Override
        public void clearSelection(int i, TableColumn tableColumn) {

        }

        @Override
        public void clearAndSelect(int i, TableColumn tableColumn) {

        }

        @Override
        public void select(int i, TableColumn tableColumn) {

        }

        @Override
        public boolean isSelected(int i, TableColumn tableColumn) {
            return false;
        }

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public ObservableList getSelectedItems() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public void selectIndices(int i, int... ints) {

        }

        @Override
        public void selectAll() {

        }

        @Override
        public void clearAndSelect(int i) {

        }

        @Override
        public void select(int i) {

        }

        @Override
        public void select(Object o) {

        }

        @Override
        public void clearSelection(int i) {

        }

        @Override
        public void clearSelection() {

        }

        @Override
        public boolean isSelected(int i) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void selectPrevious() {

        }

        @Override
        public void selectNext() {

        }

        @Override
        public void selectFirst() {

        }

        @Override
        public void selectLast() {

        }
    }
}
