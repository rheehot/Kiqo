package com.thirstygoat.kiqo.viewModel;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;

import com.thirstygoat.kiqo.model.AcceptanceCriteria;

public class AcceptanceCriteriaListCell extends ListCell<AcceptanceCriteria> {

    private Point2D dragOffset = new Point2D(0, 0);
    private ListView<AcceptanceCriteria> listView;
    
    public AcceptanceCriteriaListCell(ListView<AcceptanceCriteria> listView) {
        this.listView = listView;
    }
    
    @Override
    protected void updateItem(final AcceptanceCriteria item, final boolean empty) {
        // calling super here is very important

        if (!empty) {
            final BorderPane borderPane = new BorderPane();

            final Node handle = createHandle(item);
            borderPane.setLeft(handle);

            final Label label = new Label();
            label.textProperty().bind(item.criteria);
            label.setAlignment(Pos.BASELINE_LEFT);
            borderPane.setCenter(label);

            // borderPane.setRight(accept/reject node);

            setGraphic(borderPane);

        } else {
            // clear
            setGraphic(null);
        }
        super.updateItem(item, empty);
    }

    private Node createHandle(AcceptanceCriteria ac) {
        Label handle = new Label("H");
        
        EventHandler<DragEvent> mContextDragOver = new EventHandler<DragEvent>() {
            // dragover to handle node dragging in the right pane view
            @Override
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                relocateToPoint(new Point2D( event.getSceneX(), event.getSceneY()));
                event.consume();
            }
        };
        EventHandler<DragEvent> mContextDragDropped = new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                getParent().setOnDragOver(null);
                getParent().setOnDragDropped(null);

                // TODO retrieve actual AC
                listView.getItems().add(getIndex(), new AcceptanceCriteria(
                        ((DragContainer) event.getDragboard().getContent(DragContainer.DATA_FORMAT)).getValue("criteria")
                        ));
                event.setDropCompleted(true);

                event.consume();
            }
        };

        handle.setOnDragDetected(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                getParent().setOnDragOver(null);
                getParent().setOnDragDropped(null);

                getParent().setOnDragOver(mContextDragOver);
                getParent().setOnDragDropped(mContextDragDropped);

                // begin drag ops
//                listView.getItems().remove(getUserData())); // TODO remove AC
                dragOffset = new Point2D(event.getX(), event.getY());
                relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));

                ClipboardContent content = new ClipboardContent();
                DragContainer container = new DragContainer();
                container.addData("criteria", ac.getCriteria());
                content.put(DragContainer.DATA_FORMAT, container);
                getParent().startDragAndDrop(TransferMode.ANY).setContent(content);

                event.consume();
            }
        });
        
        return handle;
    }

    private void relocateToPoint(Point2D p) {
        // relocates the object to a point that has been converted to scene coordinates
        Point2D localCoords = getParent().sceneToLocal(p);
        relocate ((int) (localCoords.getX() - dragOffset .getX()), (int) (localCoords.getY() - dragOffset.getY()));
    }
    
    private void doStuff(AcceptanceCriteria ac, Point2D p) {
        System.out.println(ac.getCriteria() + " moved to " + p.toString());
    }
}
