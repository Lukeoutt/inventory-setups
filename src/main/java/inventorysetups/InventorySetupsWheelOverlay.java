package inventorysetups;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class InventorySetupsWheelOverlay extends Overlay
{
	public static final int MAX_SETUPS = 8;

	private static final Color SLICE_COLOR = new Color(20, 20, 20, 180);
	private static final Color SLICE_HIGHLIGHT = new Color(0, 160, 110, 210);
	private static final Color TEXT_COLOR = new Color(240, 240, 240, 220);
	private static final double CATEGORY_INNER_RADIUS_RATIO = 0.25;
	private static final double CATEGORY_OUTER_RADIUS_RATIO = 0.85;
	private static final double SETUP_INNER_RADIUS_RATIO = 0.2;
	private static final double SETUP_OUTER_RADIUS_RATIO = 0.65;
	private static final double START_ANGLE_RAD = -Math.PI / 2.0;
	private static final double SUB_RING_OFFSET_RATIO = 1.35;

	private final Client client;
	private final InventorySetupsPlugin plugin;

	private int selectedIndex = -1;
	private java.awt.Point lastCenter;
	private double lastCategoryInnerRadius;
	private double lastCategoryOuterRadius;
	private double lastSetupInnerRadius;
	private double lastSetupOuterRadius;
	private java.awt.Point lastSetupCenter;
	private int lastCategoryCount;
	private int lastSetupCount;
	private double lastStartAngleRad;

	@Inject
	public InventorySetupsWheelOverlay(Client client, InventorySetupsPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public void setActive(boolean active)
	{
		if (!active)
		{
			selectedIndex = -1;
			lastCategoryCount = 0;
			lastSetupCount = 0;
		}
	}

	public InventorySetupCategory getSelectedCategory()
	{
		List<InventorySetupCategory> categories = plugin.getWheelCategories();
		int index = getSliceIndexFromMouse(
			client.getMouseCanvasPosition(),
			lastCenter,
			lastCategoryInnerRadius,
			lastCategoryOuterRadius,
			categories.size(),
			lastStartAngleRad
		);
		if (index < 0 || index >= categories.size())
		{
			return null;
		}
		return categories.get(index);
	}

	public InventorySetup getSelectedSetup()
	{
		InventorySetupCategory category = plugin.getWheelSelectedCategory();
		if (category == null)
		{
			category = null;
		}

		List<InventorySetup> setups = plugin.getWheelSetupsForCategory(category);
		int index = getSliceIndexFromMouse(
			client.getMouseCanvasPosition(),
			lastSetupCenter,
			lastSetupInnerRadius,
			lastSetupOuterRadius,
			setups.size(),
			lastStartAngleRad
		);
		if (index < 0 || index >= setups.size())
		{
			return null;
		}

		return setups.get(index);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isWheelActive())
		{
			return null;
		}

		WheelMode mode = plugin.getWheelMode();
		if (mode == WheelMode.NONE)
		{
			return null;
		}

		List<String> labels = plugin.getWheelLabelsForMode(WheelMode.CATEGORY);
		if (labels.isEmpty())
		{
			return null;
		}

		int canvasWidth = client.getCanvasWidth();
		int canvasHeight = client.getCanvasHeight();
		int radius = Math.max(80, Math.min(canvasWidth, canvasHeight) / 4);
		int diameter = radius * 2;

		int centerX = canvasWidth / 2;
		int centerY = canvasHeight / 2;

		double categoryInnerRadius = radius * CATEGORY_INNER_RADIUS_RATIO;
		double categoryOuterRadius = radius * CATEGORY_OUTER_RADIUS_RATIO;
		double setupInnerRadius = radius * SETUP_INNER_RADIUS_RATIO;
		double setupOuterRadius = radius * SETUP_OUTER_RADIUS_RATIO;

		lastCenter = new java.awt.Point(centerX, centerY);
		lastCategoryInnerRadius = categoryInnerRadius;
		lastCategoryOuterRadius = categoryOuterRadius;
		lastSetupInnerRadius = setupInnerRadius;
		lastSetupOuterRadius = setupOuterRadius;
		lastCategoryCount = labels.size();
		lastStartAngleRad = START_ANGLE_RAD;

		Point mouse = client.getMouseCanvasPosition();
		selectedIndex = getSliceIndexFromMouse(mouse, lastCenter, lastCategoryInnerRadius, lastCategoryOuterRadius, lastCategoryCount, lastStartAngleRad);
		double hoveredAngle = getSliceCenterAngle(selectedIndex, lastCategoryCount, lastStartAngleRad);
		boolean hoveringSubRing = false;
		plugin.updateWheelHoverCategory(selectedIndex, hoveredAngle);

		int x = centerX - radius;
		int y = centerY - radius;

		graphics.setColor(SLICE_COLOR);
		graphics.fillOval(x, y, diameter, diameter);

		FontMetrics metrics = graphics.getFontMetrics();

		double sliceAngle = (Math.PI * 2) / labels.size();

		for (int i = 0; i < labels.size(); i++)
		{
			double start = lastStartAngleRad + i * sliceAngle;
			int startDeg = (int) Math.toDegrees(start);
			int arcDeg = (int) Math.toDegrees(sliceAngle);
			graphics.setColor(i == selectedIndex ? SLICE_HIGHLIGHT : SLICE_COLOR);
			graphics.fillArc(x, y, diameter, diameter, startDeg, arcDeg);

			String label = labels.get(i);
			double labelAngle = start + sliceAngle / 2;
			int labelX = (int) (centerX + Math.cos(labelAngle) * radius * 0.6);
			int labelY = (int) (centerY - Math.sin(labelAngle) * radius * 0.6);
			int textWidth = metrics.stringWidth(label);
			int textHeight = metrics.getAscent();
			graphics.setColor(TEXT_COLOR);
			graphics.drawString(label, labelX - textWidth / 2, labelY + textHeight / 2);
		}

		if (plugin.isWheelSubRingActive())
		{
			List<InventorySetup> setups = plugin.getWheelSetupsForCategory(plugin.getWheelSelectedCategory());
			lastSetupCount = setups.size();
			if (!setups.isEmpty())
			{
				double setupSliceAngle = (Math.PI * 2) / setups.size();
				double offsetAngle = plugin.getWheelSelectedAngleRad();
				double offsetDistance = radius * SUB_RING_OFFSET_RATIO;
				int setupCenterX = (int) Math.round(centerX + Math.cos(offsetAngle) * offsetDistance);
				int setupCenterY = (int) Math.round(centerY - Math.sin(offsetAngle) * offsetDistance);

				java.awt.Point setupCenter = new java.awt.Point(setupCenterX, setupCenterY);
				lastSetupCenter = setupCenter;
				int setupDiameter = (int) Math.round(setupOuterRadius * 2);
				int setupX = setupCenterX - (int) Math.round(setupOuterRadius);
				int setupY = setupCenterY - (int) Math.round(setupOuterRadius);

				int setupIndex = getSliceIndexFromMouse(mouse, setupCenter, lastSetupInnerRadius, lastSetupOuterRadius, setups.size(), lastStartAngleRad);
				hoveringSubRing = setupIndex >= 0;

				for (int i = 0; i < setups.size(); i++)
				{
					double start = lastStartAngleRad + i * setupSliceAngle;
					int startDeg = (int) Math.toDegrees(start);
					int arcDeg = (int) Math.toDegrees(setupSliceAngle);
					graphics.setColor(i == setupIndex ? SLICE_HIGHLIGHT : SLICE_COLOR);
					graphics.fillArc(setupX, setupY, setupDiameter, setupDiameter, startDeg, arcDeg);

					String label = setups.get(i).getName();
					double labelAngle = start + setupSliceAngle / 2;
					double midRadius = (setupInnerRadius + setupOuterRadius) / 2;
					int labelX = (int) (setupCenterX + Math.cos(labelAngle) * midRadius);
					int labelY = (int) (setupCenterY - Math.sin(labelAngle) * midRadius);
					int textWidth = metrics.stringWidth(label);
					int textHeight = metrics.getAscent();
					graphics.setColor(TEXT_COLOR);
					graphics.drawString(label, labelX - textWidth / 2, labelY + textHeight / 2);
				}
			}
		}

		plugin.setHoveringSubRing(hoveringSubRing);

		return null;
	}

	private int getSliceIndexFromMouse(Point mouse, java.awt.Point center, double innerRadius, double outerRadius,
		int sliceCount, double startAngleRad)
	{
		if (mouse == null || center == null || sliceCount <= 0)
		{
			return -1;
		}

		double dx = mouse.getX() - center.getX();
		double dy = center.getY() - mouse.getY();
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance < innerRadius || distance > outerRadius)
		{
			return -1;
		}

		double angle = Math.atan2(dy, dx);
		double twoPi = Math.PI * 2.0;
		double sliceAngle = twoPi / sliceCount;

		double normalized = angle - startAngleRad;
		normalized = (normalized % twoPi + twoPi) % twoPi;

		int index = (int) Math.floor(normalized / sliceAngle);
		if (index < 0 || index >= sliceCount)
		{
			return -1;
		}

		return index;
	}

	private double getSliceCenterAngle(int index, int sliceCount, double startAngleRad)
	{
		if (index < 0 || sliceCount <= 0)
		{
			return startAngleRad;
		}

		double sliceAngle = (Math.PI * 2.0) / sliceCount;
		return startAngleRad + (index + 0.5) * sliceAngle;
	}
}
