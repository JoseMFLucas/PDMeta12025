package pt.isec.pd.tp.Client.ui;

import javafx.scene.layout.Pane;

/**
 * An interface for controllers that need a reference to their root FXML node.
 */
public interface RootAwareController {
    void setRootNode(Pane rootNode);
}
