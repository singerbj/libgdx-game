package com.libgdx.helpers;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class RayCastHelper {
	Intersector intersector = new Intersector();
	
	public boolean rayTest(Vector2 shotSource, float shotAngle, float shotRange, Polygon[] rectangles) {
		//TODO:
		// check all rects to see which intersect
		//		intersector.intersectSegmentPolygon(p1, p2, polygon);
		// for those that do, get the point of intersection
		//		intersector.intersectSegments(p1, p2, p3, p4, intersection);
		// for those points, get the closest one
		return false;
	}
	
	
}
