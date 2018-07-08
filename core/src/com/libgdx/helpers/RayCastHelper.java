package com.libgdx.helpers;

import java.util.HashMap;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.Array;
import com.libgdx.game.Shot;

public class RayCastHelper {
	Intersector intersector = new Intersector();

	public Shot rayTest(Vector2 source, Vector2 dest, Array<Rectangle> array) {
		// check all rects to see which intersect
		HashMap<Segment, Rectangle> vectorRectMap = new HashMap<Segment, Rectangle>();
		for (Rectangle r : array) {
			if (Intersector.intersectSegmentPolygon(source, dest, rectangleToPolygon(r))) {
				vectorRectMap.put(new Segment(new Vector3(r.x, r.y, 0), new Vector3(r.x + r.width, r.y, 0)), r);
				vectorRectMap.put(new Segment(new Vector3(r.x + r.width, r.y, 0), new Vector3(r.x + r.width, r.y + r.height, 0)), r);
				vectorRectMap.put(new Segment(new Vector3(r.x + r.width, r.y + r.height, 0), new Vector3(r.x, r.y + r.height, 0)), r);
				vectorRectMap.put(new Segment(new Vector3(r.x, r.y + r.height, 0), new Vector3(r.x, r.y, 0)), r);
			}
		}

		// for those that do, get the point of intersection
		Segment bestIntersectionSegment = null;
		Vector2 bestIntersection = null;
		float bestDistance = -1;
		float tempDistance = -1;
		Vector2 tempIntersection;
		for (Segment key : vectorRectMap.keySet()) {
			tempIntersection = new Vector2();
			if (Intersector.intersectSegments(source.x, source.y, dest.x, dest.y, key.a.x, key.a.y, key.b.x, key.b.y,
					tempIntersection)) {
				tempDistance = distance(new Vector2(source.x, source.y), tempIntersection);

				if (bestIntersectionSegment == null || tempDistance < bestDistance) {
					bestDistance = tempDistance;
					bestIntersection = tempIntersection;
					bestIntersectionSegment = key;
				}
			}
		}
		
		return new Shot(source, bestIntersection != null ? bestIntersection : dest, vectorRectMap.get(bestIntersectionSegment));
	}

	private Polygon rectangleToPolygon(Rectangle rect) {
		float[] points = { 
				rect.x, 
				rect.y, 
				rect.x + rect.width, 
				rect.y, 
				rect.x + rect.width, 
				rect.y + rect.height,
				rect.x, 
				rect.y + rect.height
			};
		return new Polygon(points);
	}

	float distance(Vector2 object1, Vector2 object2) {
		return (float) Math.sqrt(Math.pow((object2.x - object1.x), 2) + Math.pow((object2.y - object1.y), 2));
	}

}
