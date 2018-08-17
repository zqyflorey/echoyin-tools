package com.echoyin.tools.jdbc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import cn.itcast.jdbc.TxQueryRunner;

import com.echoyin.tools.jdbc.annotation.Column;
import com.echoyin.tools.jdbc.annotation.ID;
import com.echoyin.tools.jdbc.annotation.Table;

public class BaseDAO<T> {

	private QueryRunner qr = new TxQueryRunner();
	private Class<T> beanClass;

	@SuppressWarnings("unchecked")
	public BaseDAO() {
		// <重要> 调用new
		// UserDAOImpl()后，this.getClass()为test.UserDAOImpl，再调用getGenericSuperclass()后得到的是test.BaseDAO<test.User>
		// </重要>
		ParameterizedType type = (ParameterizedType) this.getClass()
				.getGenericSuperclass();
		Type[] actualTypeArguments = type.getActualTypeArguments();
		beanClass = (Class<T>) actualTypeArguments[0];
	}

	/**
	 * add T -> database
	 * 
	 * @param bean
	 */
	public void add(T bean) {
		try {
			Field[] fs = beanClass.getDeclaredFields();

			Table table = beanClass.getAnnotation(Table.class);

			String tableString = table.value();

			String sql = "insert into " + tableString + " values(";
			for (int i = 0; i < fs.length; i++) {
				sql += "?";
				if (i < fs.length - 1) {
					sql += ",";
				}
			}
			sql += ")";
			List<Object> params = new ArrayList<Object>();
			for (Field field : fs) {
				Object num = this.getFieldValue(field, bean);
				params.add(num);
			}
			qr.update(sql, params.toArray());
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public int update(T bean) {
		ID id = null;
		Object cid = null;

		try {
			List<Object> params = new ArrayList<Object>();

			Table table = beanClass.getAnnotation(Table.class);

			Field[] fs = beanClass.getDeclaredFields();

			String sql = "update " + table.value() + " set ";

			for (int i = 0; i < fs.length; i++) {

				if (fs[i].getAnnotation(ID.class) != null) {
					id = fs[i].getAnnotation(ID.class);
					cid = getFieldValue(fs[i], bean);
				} else if (fs[i].getAnnotation(Column.class) != null) {
					String name = fs[i].getName();
					sql += name + "=?";
					if (i < fs.length - 1) {
						sql += ",";
					}
					params.add(getFieldValue(fs[i], bean));
				} else {

				}
			}

			sql += " where " + id.value() + "=?";
			params.add(cid);

			return qr.update(sql, params.toArray());
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public int delete(T bean) {
		ID id = null;
		Object cid = null;

		try {
			Field[] fields = beanClass.getDeclaredFields();

			for (Field fd : fields) {
				if (fd.getAnnotation(ID.class) != null) {
					id = fd.getAnnotation(ID.class);
					cid = getFieldValue(fd, bean);
				}
			}

			String sql = "delete from "
					+ beanClass.getAnnotation(Table.class).value() + " where "
					+ id.value() + "=?";

			return qr.update(sql, cid);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 * load
	 * 
	 * @param bean
	 * @return T
	 */
	public T load(T bean) {
		ID id = null;
		Object cid = null;

		try {
			Field[] fields = beanClass.getDeclaredFields();

			for (Field fd : fields) {
				if (fd.getAnnotation(ID.class) != null) {
					id = fd.getAnnotation(ID.class);
					cid = getFieldValue(fd, bean);
				}
			}

			String sql = "select * from "
					+ beanClass.getAnnotation(Table.class).value() + " where "
					+ id.value() + "=?";

			bean = qr.query(sql, new BeanHandler<T>(beanClass), cid);

			return bean;
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public List<T> findAll() {
		try {

			String sql = "select * from "
					+ beanClass.getAnnotation(Table.class).value();

			return qr.query(sql, new BeanListHandler<T>(beanClass));
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 * 涓�釜Field瀵瑰簲鐨勫�銆傛瘮濡俇ser#uid鐨勫�銆�
	 * 
	 * @param field
	 * @param bean
	 * @return
	 */
	private Object getFieldValue(Field field, T bean) {
		try {
			PropertyDescriptor pd = new PropertyDescriptor(field.getName(),
					beanClass);
			Method rm = pd.getReadMethod();// 鑾峰緱璇绘柟娉�
			Object num = (Object) rm.invoke(bean);
			return num;
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
}