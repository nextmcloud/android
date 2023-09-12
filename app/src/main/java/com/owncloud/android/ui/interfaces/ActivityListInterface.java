package com.owncloud.android.ui.interfaces;

import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.operations.comments.Comments;

/**
 * Created by alejandro on 12/05/17.
 */

public interface ActivityListInterface {

    void onActivityClicked(RichObject richObject);

    void onCommentsOverflowMenuClicked(Comments comments);

}
