room_format(common).
room_format(lab).
room_format(amphi).
room_format(office).
room_format(any).

room_spec(room_agent5, [id(5), format(common), seats(15), has_blackboard(yes, 1), 
						has_projector(no, _), has_tv(yes, 14), has_mic(no, _)]).
						
room_spec(room_agent6, [id(6), format(office), seats(10), has_blackboard(no, _), 
						has_projector(no, _), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent7, [id(7), format(office), seats(10), has_blackboard(yes, 2), 
						has_projector(no, _), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent8, [id(8), format(common), seats(15), has_blackboard(no, _), 
						has_projector(no, _), has_tv(yes, 15), has_mic(yes, 16)]).

room_spec(room_agent1, [id(1), format(amphi), seats(100), has_blackboard(yes, 3), 
						has_projector(yes, 12), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent2, [id(2), format(amphi), seats(120), has_blackboard(yes, 4), 
						has_projector(yes, 13), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent3, [id(3), format(lab), seats(18), has_blackboard(yes, 5), 
						has_projector(no, _), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent4, [id(4), format(lab), seats(18), has_blackboard(yes, 6), 
						has_projector(no, _), has_tv(no, _), has_mic(no, _)]).
						
room_spec(room_agent9, [id(9), format(office), seats(6), has_blackboard(yes, 7), 
						has_projector(no, _), has_tv(no, _), has_mic(no, _)]).