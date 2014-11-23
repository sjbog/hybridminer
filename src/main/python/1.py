# coding=utf-8
from __future__ import print_function

__author__ = 'Bogdan'

from collections import defaultdict
from pprint import pprint


def x ( start_node )	:
	branches	= set ( successors [ start_node ] )
	# print ( len ( branches ), branches )

	branch_elimination	= dict ()

	for	node in branches	:
		branch_elimination [ node ]	= set ( predecessors.get ( node, set () ) )
		branch_elimination [ node ]	= branch_elimination [ node ] - set( [ start_node ] ) - branches

	pprint ( branch_elimination )


	print ()
	print ( "Graph:" )
	graph	= find_structure ( start_node, branches )
	pprint ( graph )
	print ()


def find_structure ( start_node, branches )	:

	and_node	= '__and__'
	xor_node	= '__xor__'

	graph	= [ start_node ]
	and_nodes	= {}
	xor_nodes	= {}

	for	branch_node in branches	:

		_and_nodes	= branches.intersection ( predecessors [ branch_node ] )
		_and_nodes	= _and_nodes.intersection ( successors [ branch_node ] )

		and_nodes [ branch_node ]	= _and_nodes

		_xor_nodes	= branches.difference ( predecessors [ branch_node ] )
		_xor_nodes	= _xor_nodes.difference ( successors [ branch_node ] )
		_xor_nodes.remove ( branch_node )

		xor_nodes [ branch_node ]	= _xor_nodes

	graph.append ( { and_node : and_nodes } )
	graph.append ( { xor_node : xor_nodes } )

	# pprint ( and_nodes )

	construct ( xor_nodes )

	return	graph

def construct ( xor_nodes )	:
	visited_nodes	= set ()

	# for	node

	# fringe	= list ( xor_nodes.keys () )

	# while	fringe	:
	# 	node	= fringe.pop ()

	# 	if	node in visited_nodes	:
	# 		continue

	# 	same_lvl_nodes	= xor_nodes [ node ] + set ( node )



if __name__ == "__main__"	:
	predecessors	= {"Send Notification by e-mail":["Receive Questionnaire Response","Create Questionnaire","Send Notification by Phone","Prepare Notification Content","Send Notification by Post","Send Questionnaire"],"Skip Questionnaire":["Send Questionnaire"],"High Insurance Check":["Create Questionnaire","Contact Hospital","Register","High Medical History"],"Archive":["Receive Questionnaire Response","Send Notification by e-mail","Skip Questionnaire","Send Notification by Phone","Send Notification by Post"],"Send Notification by Phone":["Send Notification by e-mail","Create Questionnaire","Prepare Notification Content","Send Notification by Post","Send Questionnaire"],"Prepare Notification Content":["Receive Questionnaire Response","Create Questionnaire","High Insurance Check","Contact Hospital","High Medical History","Low Insurance Check","Low Medical History","Send Questionnaire"],"Send Notification by Post":["Receive Questionnaire Response","Send Notification by e-mail","Create Questionnaire","Send Notification by Phone","Prepare Notification Content","Send Questionnaire"],"Send Questionnaire":["Send Notification by e-mail","Create Questionnaire","Send Notification by Phone","Prepare Notification Content","Send Notification by Post","Low Insurance Check"],"Low Medical History":["Create Questionnaire","Register","Low Insurance Check"],"Receive Questionnaire Response":["Send Notification by e-mail","Send Notification by Phone","Prepare Notification Content","Send Notification by Post","Send Questionnaire"],"Create Questionnaire":["Send Notification by e-mail","High Insurance Check","Send Notification by Phone","Contact Hospital","Register","Prepare Notification Content","High Medical History","Send Notification by Post","Low Insurance Check","Low Medical History"],"Contact Hospital":["Create Questionnaire","High Insurance Check","Register","High Medical History"],"High Medical History":["Create Questionnaire","High Insurance Check","Register","Contact Hospital"],"Low Insurance Check":["Create Questionnaire","Register","Low Medical History"]}
	successors		= {"Send Notification by e-mail":["Receive Questionnaire Response","Create Questionnaire","Send Notification by Phone","Archive","Send Notification by Post","Send Questionnaire"],"Skip Questionnaire":["Archive"],"High Insurance Check":["Create Questionnaire","Contact Hospital","Prepare Notification Content","High Medical History"],"Send Notification by Phone":["Send Notification by e-mail","Receive Questionnaire Response","Create Questionnaire","Archive","Send Notification by Post","Send Questionnaire"],"Register":["Create Questionnaire","High Insurance Check","Contact Hospital","High Medical History","Low Insurance Check","Low Medical History"],"Prepare Notification Content":["Send Notification by e-mail","Receive Questionnaire Response","Create Questionnaire","Send Notification by Phone","Send Notification by Post","Send Questionnaire"],"Send Notification by Post":["Send Notification by e-mail","Receive Questionnaire Response","Create Questionnaire","Send Notification by Phone","Archive","Send Questionnaire"],"Send Questionnaire":["Receive Questionnaire Response","Send Notification by e-mail","Skip Questionnaire","Send Notification by Phone","Prepare Notification Content","Send Notification by Post"],"Low Medical History":["Create Questionnaire","Prepare Notification Content","Low Insurance Check"],"Receive Questionnaire Response":["Send Notification by e-mail","Archive","Prepare Notification Content","Send Notification by Post"],"Create Questionnaire":["Send Notification by e-mail","High Insurance Check","Send Notification by Phone","Contact Hospital","Prepare Notification Content","High Medical History","Send Notification by Post","Low Insurance Check","Send Questionnaire","Low Medical History"],"Contact Hospital":["Create Questionnaire","High Insurance Check","Prepare Notification Content","High Medical History"],"High Medical History":["Create Questionnaire","High Insurance Check","Contact Hospital","Prepare Notification Content"],"Low Insurance Check":["Create Questionnaire","Prepare Notification Content","Low Medical History","Send Questionnaire"]}

	x ( "Register" )
	# print ( len ( predecessors [ 'Create Questionnaire' ] ), predecessors [ 'Create Questionnaire' ] )

	successors	= {
		"start" : [ 'a', 'd', 'g' ],

		'a' : [ 'b', 'd', 'e', 'f', 'g','h','l' ],
		'b' : [ 'c', 'd', 'e', 'f', 'g','h','l' ],
		'c' : [ 'd', 'e', 'f', 'g','h','l' ],

		'd' : [ 'a', 'b', 'c', 'e', 'g','h','l' ],
		'e' : [ 'a', 'b', 'c', 'f', 'g','h','l' ],
		'f' : [ 'a', 'b', 'c', 'g','h','l' ],

		'g' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'h' ],
		'h' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'l' ],
		'l' : [ 'a', 'b', 'c', 'd', 'e', 'f' ],
	}

	predecessors	= {
		"start" : [],

		'a' : [ 'start', 'd', 'e', 'f', 'g','h','l' ],
		'b' : [ 'a', 'd', 'e', 'f', 'g','h','l' ],
		'c' : [ 'b', 'd', 'e', 'f', 'g','h','l' ],

		'd' : [ 'a', 'b', 'c', 'start', 'g','h','l' ],
		'e' : [ 'a', 'b', 'c', 'd', 'g','h','l' ],
		'f' : [ 'a', 'b', 'c', 'e','h','l' ],

		'g' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'start' ],
		'h' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'g' ],
		'l' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'h' ],
	}

	x ( 'start' )


	successors	= {
		"start" : [ 'a', 'd', 'g' ],

		'a' : [ 'b', 'g','h','l' ],
		'b' : [ 'c', 'g','h','l' ],
		'c' : [ 'g','h','l' ],

		'd' : [ 'e', 'g','h','l' ],
		'e' : [ 'f', 'g','h','l' ],
		'f' : [ 'g','h','l' ],

		'g' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'h' ],
		'h' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'l' ],
		'l' : [ 'a', 'b', 'c', 'd', 'e', 'f' ],
	}

	predecessors	= {
		"start" : [],

		'a' : [ 'start', 'g','h','l' ],
		'b' : [ 'a', 'g','h','l' ],
		'c' : [ 'b', 'g','h','l' ],

		'd' : [ 'start', 'g','h','l' ],
		'e' : [ 'd', 'g','h','l' ],
		'f' : [ 'e','h','l' ],

		'g' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'start' ],
		'h' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'g' ],
		'l' : [ 'a', 'b', 'c', 'd', 'e', 'f', 'h' ],
	}

	x ( 'start' )

