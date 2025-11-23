package server;

import java.sql.*;

/**
 * Wrapper for pooled connections that returns connection to pool when closed
 */
public class PooledConnection implements Connection {
    private final Connection realConnection;
    private final DatabasePool pool;
    private boolean closed = false;
    
    public PooledConnection(Connection realConnection, DatabasePool pool) {
        this.realConnection = realConnection;
        this.pool = pool;
    }
    
    @Override
    public void close() throws SQLException {
        if (!closed) {
            closed = true;
            pool.returnConnection(realConnection);
        }
    }
    
    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }
    
    // Delegate all other methods to real connection
    @Override
    public Statement createStatement() throws SQLException {
        checkClosed();
        return realConnection.createStatement();
    }
    
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkClosed();
        return realConnection.prepareStatement(sql);
    }
    
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        checkClosed();
        return realConnection.prepareCall(sql);
    }
    
    @Override
    public String nativeSQL(String sql) throws SQLException {
        checkClosed();
        return realConnection.nativeSQL(sql);
    }
    
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        realConnection.setAutoCommit(autoCommit);
    }
    
    @Override
    public boolean getAutoCommit() throws SQLException {
        checkClosed();
        return realConnection.getAutoCommit();
    }
    
    @Override
    public void commit() throws SQLException {
        checkClosed();
        realConnection.commit();
    }
    
    @Override
    public void rollback() throws SQLException {
        checkClosed();
        realConnection.rollback();
    }
    
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkClosed();
        return realConnection.getMetaData();
    }
    
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkClosed();
        realConnection.setReadOnly(readOnly);
    }
    
    @Override
    public boolean isReadOnly() throws SQLException {
        checkClosed();
        return realConnection.isReadOnly();
    }
    
    @Override
    public void setCatalog(String catalog) throws SQLException {
        checkClosed();
        realConnection.setCatalog(catalog);
    }
    
    @Override
    public String getCatalog() throws SQLException {
        checkClosed();
        return realConnection.getCatalog();
    }
    
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        checkClosed();
        realConnection.setTransactionIsolation(level);
    }
    
    @Override
    public int getTransactionIsolation() throws SQLException {
        checkClosed();
        return realConnection.getTransactionIsolation();
    }
    
    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkClosed();
        return realConnection.getWarnings();
    }
    
    @Override
    public void clearWarnings() throws SQLException {
        checkClosed();
        realConnection.clearWarnings();
    }
    
    // Additional methods required by Connection interface
    @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { checkClosed(); return realConnection.createStatement(resultSetType, resultSetConcurrency); }
    @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { checkClosed(); return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { checkClosed(); return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency); }
    @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { checkClosed(); return realConnection.getTypeMap(); }
    @Override public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException { checkClosed(); realConnection.setTypeMap(map); }
    @Override public void setHoldability(int holdability) throws SQLException { checkClosed(); realConnection.setHoldability(holdability); }
    @Override public int getHoldability() throws SQLException { checkClosed(); return realConnection.getHoldability(); }
    @Override public Savepoint setSavepoint() throws SQLException { checkClosed(); return realConnection.setSavepoint(); }
    @Override public Savepoint setSavepoint(String name) throws SQLException { checkClosed(); return realConnection.setSavepoint(name); }
    @Override public void rollback(Savepoint savepoint) throws SQLException { checkClosed(); realConnection.rollback(savepoint); }
    @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { checkClosed(); realConnection.releaseSavepoint(savepoint); }
    @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { checkClosed(); return realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); }
    @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { checkClosed(); return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { checkClosed(); return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
    @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { checkClosed(); return realConnection.prepareStatement(sql, autoGeneratedKeys); }
    @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { checkClosed(); return realConnection.prepareStatement(sql, columnIndexes); }
    @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { checkClosed(); return realConnection.prepareStatement(sql, columnNames); }
    @Override public Clob createClob() throws SQLException { checkClosed(); return realConnection.createClob(); }
    @Override public Blob createBlob() throws SQLException { checkClosed(); return realConnection.createBlob(); }
    @Override public NClob createNClob() throws SQLException { checkClosed(); return realConnection.createNClob(); }
    @Override public SQLXML createSQLXML() throws SQLException { checkClosed(); return realConnection.createSQLXML(); }
    @Override public boolean isValid(int timeout) throws SQLException { return !closed && realConnection.isValid(timeout); }
    @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { try { checkClosed(); realConnection.setClientInfo(name, value); } catch (SQLException e) { throw new SQLClientInfoException(); } }
    @Override public void setClientInfo(java.util.Properties properties) throws SQLClientInfoException { try { checkClosed(); realConnection.setClientInfo(properties); } catch (SQLException e) { throw new SQLClientInfoException(); } }
    @Override public String getClientInfo(String name) throws SQLException { checkClosed(); return realConnection.getClientInfo(name); }
    @Override public java.util.Properties getClientInfo() throws SQLException { checkClosed(); return realConnection.getClientInfo(); }
    @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { checkClosed(); return realConnection.createArrayOf(typeName, elements); }
    @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { checkClosed(); return realConnection.createStruct(typeName, attributes); }
    @Override public void setSchema(String schema) throws SQLException { checkClosed(); realConnection.setSchema(schema); }
    @Override public String getSchema() throws SQLException { checkClosed(); return realConnection.getSchema(); }
    @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { checkClosed(); realConnection.abort(executor); }
    @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException { checkClosed(); realConnection.setNetworkTimeout(executor, milliseconds); }
    @Override public int getNetworkTimeout() throws SQLException { checkClosed(); return realConnection.getNetworkTimeout(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { checkClosed(); return realConnection.unwrap(iface); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { checkClosed(); return realConnection.isWrapperFor(iface); }
    
    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("Connection is closed");
        }
    }
}