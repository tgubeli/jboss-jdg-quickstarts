package org.jboss.as.quickstarts.datagrid.helloworld;

import java.io.Serializable;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Indexed
public class JSONIndexedWrapper implements Serializable{

	private static final long serialVersionUID = 1L;	
	
	@Field String jsonString;
	
}
