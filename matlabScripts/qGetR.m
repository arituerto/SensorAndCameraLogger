function R = qGetR( Qrotation )
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
	w = Qrotation( 4 );
	x = Qrotation( 1 );
	y = Qrotation( 2 );
	z = Qrotation( 3 );
end

Rxx = w^2 + x^2 - y^2 - z^2;
Rxy = 2*(x*y - z*w);
Rxz = 2*(x*z + y*w);

Ryx = 2*(x*y + z*w);
Ryy = w^2 - x^2 + y^2 - z^2;
Ryz = 2*(y*z - x*w );

Rzx = 2*(x*z - y*w );
Rzy = 2*(y*z + x*w );
Rzz = w^2 - x^2 - y^2 + z^2;

R = [ 
    Rxx,    Rxy,    Rxz;
    Ryx,    Ryy,    Ryz;
    Rzx,    Rzy,    Rzz];