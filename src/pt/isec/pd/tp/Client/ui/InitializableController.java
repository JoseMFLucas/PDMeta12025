package pt.isec.pd.tp.Client.ui;

import pt.isec.pd.tp.Client.logica.ClientManager;

/**
 * A custom interface to allow controllers to be initialized with the ClientManager
 * after the FXML has been loaded.
 */
public interface InitializableController {
    void init(ClientManager clientManager);
}
