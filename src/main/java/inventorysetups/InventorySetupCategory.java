package inventorysetups;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class InventorySetupCategory
{
	@Getter
	@Setter
	private String id;

	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private List<String> setupNames = new ArrayList<>();

	public InventorySetupCategory()
	{
	}

	public InventorySetupCategory(String id, String name, List<String> setupNames)
	{
		this.id = id;
		this.name = name;
		if (setupNames != null)
		{
			this.setupNames = setupNames;
		}
	}
}
