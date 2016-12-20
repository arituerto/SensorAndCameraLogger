function [Theta, v] = qGetThetaV( Qrotation )
% qGetR: get a 3x3 rotation matrix
% R = qGetR( Qrotation )
% IN: 
%     Qrotation - quaternion describing rotation
% 
% OUT:
%     R - rotation matrix 
%     
% VERSION: 03.03.2012

if length(Qrotation) == 3
	x = Qrotation( 1 );
	y = Qrotation( 2 );
	z = Qrotation( 3 );
	sin_theta_2 = sqrt(x^2 + y^2 + z^2);
	cos_theta_2 = cos(asin(sin_theta_2));
	w = cos_theta_2;
else
	w = Qrotation( 1 );
	x = Qrotation( 2 );
	y = Qrotation( 3 );
	z = Qrotation( 4 );
end

Theta = 2*acos(w);
v = [x y z]/sin(acos(w));
v = v/norm(v);