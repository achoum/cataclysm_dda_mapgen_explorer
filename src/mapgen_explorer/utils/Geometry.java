
package mapgen_explorer.utils;

public class Geometry {

	public static abstract class PointCallback {
		public abstract void iter(int x, int y);
	}

	// Call "callback" on each point in the line between "a" and "b".
	public static boolean callOnLine(Vector2i a, Vector2i b, PointCallback callback) {
		int abs_delta_x = b.x - a.x;
		// if x1 == x2, then it does not matter what we set here
		int sign_delta_x = Integer.signum(abs_delta_x);
		abs_delta_x = Math.abs(abs_delta_x) << 1;
		int abs_delta_y = b.y - a.y;
		// if y1 == y2, then it does not matter what we set here
		int sign_delta_y = Integer.signum(abs_delta_y);
		abs_delta_y = Math.abs(abs_delta_y) << 1;
		int ax = a.x;
		int ay = a.y;
		if (abs_delta_x >= abs_delta_y) {
			//ay += sign_delta_y;
			int error = /*delta_y*/ -(abs_delta_x >> 1);
			while (ax != b.x) {
				if (error >= 0 && (error != 0 || sign_delta_x > 0)) {
					error -= abs_delta_x;
					ay += sign_delta_y;
					callback.iter(ax, ay);
				}
				error += abs_delta_y;
				ax += sign_delta_x;
				callback.iter(ax, ay);
			}
		} else {
			//ax += sign_delta_x;
			int error = /*delta_x*/ -(abs_delta_y >> 1);
			while (ay != b.y) {
				if (error >= 0 && (error != 0 || sign_delta_y > 0)) {
					error -= abs_delta_y;
					ax += sign_delta_x;
					callback.iter(ax, ay);
				}
				error += abs_delta_x;
				ay += sign_delta_y;
				callback.iter(ax, ay);
			}
		}
		return true;
	}

}
