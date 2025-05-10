package garretreichenbach.taskprocessor.util;

import org.json.JSONObject;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public interface JSONSerializable {

	JSONObject toJSON();

	void fromJSON(JSONObject jsonObject);
}
